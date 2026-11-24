package zel.room

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import zel.ext.ExtensionBus
import zel.ext.ServerEvent
import zel.room.tick.PlayerChannels
import zel.room.tick.RoomClock
import zel.room.tick.RoomTick
import zel.room.tick.RoomTicker
import zel.room.tick.RoomWorkers
import zel.room.tick.TickOutput
import zel.room.tick.TickReport
import zel.room.unit.RoomUnit
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap

@Suppress("TooManyFunctions") // load, lookup and unload of every room in one place
class RoomRegistry(
    private val infos: (Int) -> RoomInfo?,
    private val floors: FloorSource,
    private val parts: List<RoomPart>,
    private val listeners: List<RoomListener>,
    private val tick: RoomTick,
    private val workers: RoomWorkers,
    private val channels: PlayerChannels,
    private val clock: RoomClock = RoomClock.System,
    private val report: TickReport = TickReport.Log,
    /** Waits for the room's earlier writes, so a load never reads rows its last unload has yet to save. */
    private val settle: (Int) -> Unit = {},
) : RoomDirectory {
    private val rooms = ConcurrentHashMap<Int, LoadedRoom>()
    private val supplied = ConcurrentHashMap<Int, FloorSource>()
    private val loading = ConcurrentHashMap<Int, CompletableFuture<LoadedRoom?>>()
    private val playerRooms = ConcurrentHashMap<Int, Int>()

    fun loaded(roomId: Int): Room? = rooms[roomId]?.room

    /** The room's floor comes from [source] from its next load on. */
    fun supplyFloor(
        roomId: Int,
        source: FloorSource,
    ) {
        supplied[roomId] = source
    }

    fun load(roomId: Int): Room? {
        rooms[roomId]?.let { existing ->
            if (existing.room.locked { it.state == RoomState.Loaded }) return existing.room
            rooms.remove(roomId, existing)
        }
        val mine = CompletableFuture<LoadedRoom?>()
        val running = loading.putIfAbsent(roomId, mine)
        if (running != null) return running.join()?.room
        return loadAs(roomId, mine)?.room
    }

    fun <T> withRoom(
        roomId: Int,
        block: (RoomScope) -> T,
    ): T? {
        val loaded = rooms[roomId] ?: return null
        return loaded.room.locked { room ->
            if (room.state != RoomState.Loaded) return@locked null
            loaded.output.beginBatch()
            try {
                block(loaded.scope)
            } finally {
                loaded.output.endBatch()
            }
        }
    }

    fun <T> withRoomOf(
        playerId: Int,
        block: (RoomScope) -> T,
    ): T? = roomOf(playerId)?.let { withRoom(it, block) }

    fun <T> withPlayer(
        playerId: Int,
        block: (RoomScope, RoomUnit) -> T,
    ): T? =
        withRoomOf(playerId) { scope ->
            scope.room.units
                .ofPlayer(playerId)
                ?.let { block(scope, it) }
        }

    fun post(
        roomId: Int,
        delayMillis: Long = 0,
        block: (RoomScope) -> Unit,
    ): Boolean = workers.post(roomId, delayMillis) { withRoom(roomId, block) }

    fun enter(
        playerId: Int,
        roomId: Int,
    ) {
        val before = playerRooms.put(playerId, roomId)
        if (before == roomId) return
        before?.let { ExtensionBus.event(ServerEvent.LeftRoom(playerId, it)) }
        ExtensionBus.event(ServerEvent.EnteredRoom(playerId, roomId))
    }

    /** A late call for a room the player already left keeps its new room. */
    fun leave(
        playerId: Int,
        roomId: Int,
    ) {
        if (playerRooms.remove(playerId, roomId)) ExtensionBus.event(ServerEvent.LeftRoom(playerId, roomId))
    }

    fun unloadIfEmpty(roomId: Int): Boolean =
        workers.post(roomId) {
            val loaded = rooms[roomId] ?: return@post
            takeDown(loaded, onlyIfEmpty = true)
        }

    fun unload(roomId: Int) {
        val loaded = rooms[roomId] ?: return
        takeDown(loaded)
    }

    fun shutdown() {
        val all = rooms.values.toList()
        // Freeze every room before the first one unloads, so no room ticks against a half-unloaded neighbour.
        all.forEach { it.ticker.stop() }
        all.forEach(::takeDown)
        workers.shutdown()
    }

    override fun isLoaded(roomId: Int): Boolean = rooms.containsKey(roomId)

    override fun loadedRoomIds(): Set<Int> = rooms.keys.toSet()

    override fun roomOf(playerId: Int): Int? = playerRooms[playerId]

    override fun playersIn(roomId: Int): List<Int> = rooms[roomId]?.room?.locked { it.units.playerIds() }.orEmpty()

    /** Completes [mine] whatever happens, so the callers waiting on this load are never left hanging. */
    @Suppress("TooGenericExceptionCaught") // a failed load is logged and its waiters still get an answer
    private fun loadAs(
        roomId: Int,
        mine: CompletableFuture<LoadedRoom?>,
    ): LoadedRoom? =
        try {
            val loaded = rooms[roomId] ?: build(roomId)?.also { rooms[roomId] = it }
            mine.complete(loaded)
            loaded?.ticker?.start(workers)
            loaded
        } catch (e: Exception) {
            log.error("room {}: load failed", roomId, e)
            null
        } finally {
            mine.complete(null)
            loading.remove(roomId, mine)
        }

    @Suppress("TooGenericExceptionCaught") // loaded parts still unload
    private fun build(roomId: Int): LoadedRoom? {
        settle(roomId)
        val info = infos(roomId) ?: return null
        val floor = (supplied[roomId] ?: floors).floorOf(info)
        if (floor == null) {
            log.warn("room {}: no floor for model '{}'; not loaded", roomId, info.modelName)
            return null
        }
        val room = Room(info, floor)
        val output = TickOutput(room, channels)
        val scope = EngineScope(room, output, clock, listeners)
        val loaded = LoadedRoom(room, output, RoomTicker(room, output, tick, scope, clock, report), scope)
        room.locked {
            val done = ArrayList<RoomPart>(parts.size)
            try {
                parts.forEach { part ->
                    part.load(room)
                    done += part
                }
            } catch (e: Exception) {
                room.moveTo(RoomState.Unloading)
                unloadParts(loaded, done)
                room.moveTo(RoomState.Unloaded)
                throw e
            }
            room.moveTo(RoomState.Loaded)
        }
        return loaded
    }

    private fun takeDown(
        loaded: LoadedRoom,
        onlyIfEmpty: Boolean = false,
    ): Boolean {
        val room = loaded.room
        val went =
            room.locked {
                if (room.state != RoomState.Loaded) return@locked true
                if (onlyIfEmpty && room.units.playerIds().isNotEmpty()) return@locked false
                loaded.ticker.stop()
                room.moveTo(RoomState.Unloading)
                loaded.output.dropHeld()
                unloadParts(loaded, parts)
                playerRooms.values.removeIf { id -> id == room.info.id }
                room.moveTo(RoomState.Unloaded)
                true
            }
        if (went) rooms.remove(room.info.id, loaded)
        return went
    }

    @Suppress("TooGenericExceptionCaught") // one failing part must not stop the others unloading
    private fun unloadParts(
        loaded: LoadedRoom,
        toUnload: List<RoomPart>,
    ) {
        loaded.output.beginBatch()
        toUnload.forEach { part ->
            try {
                part.unload(loaded.scope)
            } catch (e: Exception) {
                log.error("room {}: unload step {} failed", loaded.room.info.id, part.javaClass.simpleName, e)
            }
        }
        loaded.output.endBatch()
    }

    private class LoadedRoom(
        val room: Room,
        val output: TickOutput,
        val ticker: RoomTicker,
        val scope: RoomScope,
    )

    private companion object {
        val log: Logger = LoggerFactory.getLogger(RoomRegistry::class.java)
    }
}
