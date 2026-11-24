package zel.room.entry

import zel.core.rules.rules
import zel.game.players.Player
import zel.protocol.generated.CloseConnectionOut
import zel.protocol.generated.CloseConnectionOutCodec
import zel.protocol.generated.FlatAccessDeniedOut
import zel.protocol.generated.FlatAccessDeniedOutCodec
import zel.protocol.generated.RoomForwardOut
import zel.protocol.generated.RoomForwardOutCodec
import zel.protocol.generated.RoomQueueStatusOutCodec
import zel.protocol.generated.RoomUnitRemoveOut
import zel.protocol.generated.RoomUnitRemoveOutCodec
import zel.protocol.generated.YouAreNotSpectatorOut
import zel.protocol.generated.YouAreNotSpectatorOutCodec
import zel.protocol.generated.YouArePlayingGameOut
import zel.protocol.generated.YouArePlayingGameOutCodec
import zel.room.Room
import zel.room.RoomPart
import zel.room.RoomScope
import zel.room.event.RoomEvent
import zel.room.tick.Audience
import zel.room.tick.Lane
import zel.room.tick.TickPart
import zel.room.unit.RoomUnit
import zel.room.unit.UnitKind
import zel.room.unit.walk.WalkHooks
import zel.util.SECONDS_PER_MINUTE
import java.util.concurrent.ConcurrentHashMap

class RoomLeaving(
    private val left: (playerId: Int, roomId: Int) -> Unit,
    private val environment: EntryEnvironment,
    private val store: EntryStore,
    private val spectatorSlots: () -> Int = { rules.roomSpectatorSlots },
    private val ownerLeft: (RoomScope, Int) -> Unit = { _, _ -> },
) : RoomPart,
    WalkHooks {
    var promoteSpectator: (RoomScope, Int) -> Unit = { _, _ -> }

    private val doors = ConcurrentHashMap<Room, RoomDoor>()

    private val pending = ConcurrentHashMap<Int, Int>()

    val tick: TickPart = TickPart { scope -> dropOffline(scope) }

    override fun load(room: Room) {
        doors[room] = RoomDoor()
    }

    override fun unload(scope: RoomScope) {
        val door = doors.remove(scope.room) ?: return
        (door.ringing.keys + door.queue.all().map { it.playerId }).forEach { pending.remove(it, scope.room.info.id) }
    }

    fun doorOf(room: Room): RoomDoor = doors.getOrPut(room) { RoomDoor() }

    fun pendingRoom(playerId: Int): Int? = pending[playerId]

    fun markPending(
        playerId: Int,
        roomId: Int,
    ) {
        pending[playerId] = roomId
    }

    fun clearPending(
        playerId: Int,
        roomId: Int,
    ) {
        pending.remove(playerId, roomId)
    }

    override fun leave(
        scope: RoomScope,
        unit: RoomUnit,
    ) {
        val playerId = (unit.kind as? UnitKind.Player)?.playerId ?: return
        drop(scope, playerId)
    }

    /** YouArePlayingGame false on the tick before the door, CloseConnection 0.5 s on. */
    override fun leavingByDoor(
        scope: RoomScope,
        unit: RoomUnit,
    ) {
        val playerId = (unit.kind as? UnitKind.Player)?.playerId ?: return
        val door = doorOf(scope.room)
        if (!door.present.containsKey(playerId) || playerId in door.quitting) return
        door.leavingByDoor += playerId
        scope.out.send(Lane.Now, Audience.Player(playerId), YouArePlayingGameOutCodec, YouArePlayingGameOut(false))
    }

    fun drop(
        scope: RoomScope,
        playerId: Int,
    ): Boolean {
        val door = doorOf(scope.room)
        if (!door.present.containsKey(playerId)) return false
        val closed = door.quitting.remove(playerId)
        val told = door.leavingByDoor.remove(playerId)
        val to = Audience.Player(playerId)
        if (!closed && !told) {
            scope.out.send(Lane.Now, to, YouArePlayingGameOutCodec, YouArePlayingGameOut(false))
        }
        remove(scope, playerId, toSelf = false)
        if (!closed) scope.out.send(Lane.Now, to, CloseConnectionOutCodec, CloseConnectionOut)
        return true
    }

    fun remove(
        scope: RoomScope,
        playerId: Int,
        toSelf: Boolean,
    ) {
        val room = scope.room
        val door = doorOf(room)
        val present = door.present.remove(playerId) ?: return
        door.quitting.remove(playerId)
        door.leavingByDoor.remove(playerId)
        door.admittedSpectators.remove(playerId)
        val leftQueue = door.queue.remove(playerId) != null
        room.spectators -= playerId
        if (!present.spectator) {
            // The leave trigger fires while the unit and its variables are still here.
            room.units.whileLeaving(playerId) { scope.raise(RoomEvent.Leaving(present.unitIndex)) }
            val removal = RoomUnitRemoveOut(present.unitIndex.toString())
            if (toSelf) scope.out.send(Lane.Now, Audience.Player(playerId), RoomUnitRemoveOutCodec, removal)
            room.units.remove(present.unitIndex)
            scope.out.send(Lane.Now, Audience.Room, RoomUnitRemoveOutCodec, removal)
        }
        ownerLeft(scope, playerId)
        if (!present.spectator) scope.raise(RoomEvent.Left(present.unitIndex))
        if (!toSelf) left(playerId, room.info.id)
        environment.player(playerId)?.let(environment::left)
        val roomId = room.info.id
        val ownerId = room.info.ownerId
        environment.persistInOrder(VisitKey(roomId, playerId)) {
            val now = environment.nowSeconds()
            val entered = store.openVisitStart(roomId, playerId)
            store.logExit(roomId, playerId, now)
            if (entered != null && ownerId != playerId) {
                environment.visitEnded(ownerId, present.enteredLevel, (now - entered) / SECONDS_PER_MINUTE)
            }
        }
        ownerStatus(scope)
        if (leftQueue) sendQueueStatus(scope, door)
        admitFromQueue(scope, door)
        admitSpectators(scope, door)
    }

    fun ownerStatus(scope: RoomScope) {
        val ownerId = scope.room.info.ownerId
        if (doorOf(
                scope.room,
            ).present.containsKey(ownerId)
        ) {
            environment.player(ownerId)?.let(environment::buildPassStatus)
        }
    }

    fun emptyForRebuild(
        scope: RoomScope,
        firstId: Int,
    ): List<Int> {
        val door = doorOf(scope.room)
        val roomId = scope.room.info.id
        val waiting = door.ringing.keys.toList()
        door.ringing.clear()
        waiting.forEach { clearPending(it, roomId) }
        val leavers = listOf(firstId) + door.present.keys.filter { it != firstId }
        val others = leavers.filter { it != firstId }
        // Spectators go first so that no free place promotes one of them on the way out.
        leavers.sortedByDescending { door.present[it]?.spectator == true }.forEach { drop(scope, it) }
        return others + waiting
    }

    fun deny(
        scope: RoomScope,
        playerId: Int,
    ) = scope.out.send(
        Lane.Now,
        Audience.Player(playerId),
        FlatAccessDeniedOutCodec,
        FlatAccessDeniedOut(scope.room.info.id, ""),
    )

    private fun admitFromQueue(
        scope: RoomScope,
        door: RoomDoor,
    ) {
        val roomId = scope.room.info.id
        val first = door.queue.all().firstOrNull()
        when {
            first != null && door.visitors() < first.entry.room.maxUsers && door.present[first.playerId] != null -> {
                door.queue.remove(first.playerId)
                notSpectator(scope, first.playerId)
                promoteSpectator(scope, first.playerId)
            }

            first != null && door.visitors() < first.entry.room.maxUsers -> {
                door.queue.remove(first.playerId)
                clearPending(first.playerId, roomId)
                if (first.spectator) notSpectator(scope, first.playerId)
                scope.out.send(
                    Lane.Now,
                    Audience.Player(first.playerId),
                    RoomForwardOutCodec,
                    RoomForwardOut(roomId),
                )
            }

            first == null -> {
                val watching = door.firstSpectator() ?: return
                if (door.visitors() < scope.room.info.maxUsers) {
                    notSpectator(scope, watching)
                    promoteSpectator(scope, watching)
                }
                return
            }
        }
        sendQueueStatus(scope, door)
    }

    fun spectatorPlaceFree(door: RoomDoor): Boolean =
        spectatorSlots() - door.spectators() - door.admittedSpectators.size > 0

    fun admitSpectators(
        scope: RoomScope,
        door: RoomDoor,
    ) {
        val roomId = scope.room.info.id
        val free = spectatorSlots() - door.spectators() - door.admittedSpectators.size
        val admitted =
            door.queue
                .all()
                .filter { it.spectator }
                .take(free.coerceAtLeast(0))
        admitted.forEach { queued ->
            door.queue.remove(queued.playerId)
            door.admittedSpectators += queued.playerId
            scope.out.send(Lane.Now, Audience.Player(queued.playerId), RoomForwardOutCodec, RoomForwardOut(roomId))
        }
        if (admitted.isNotEmpty()) sendQueueStatus(scope, door)
    }

    fun sendQueueStatus(
        scope: RoomScope,
        door: RoomDoor,
    ) {
        val roomId = scope.room.info.id
        door.queue.all().forEach { queued ->
            val status = EntryMessages.queueStatus(roomId, queued.spectator, door.queue.ahead(queued.playerId))
            scope.out.send(Lane.Now, Audience.Player(queued.playerId), RoomQueueStatusOutCodec, status)
        }
    }

    fun sendQueueStatus(
        scope: RoomScope,
        door: RoomDoor,
        playerId: Int,
    ) {
        val queued = door.queue.all().firstOrNull { it.playerId == playerId }
        val status =
            EntryMessages.queueStatus(
                scope.room.info.id,
                spectator = queued?.spectator ?: true,
                ahead = if (queued == null) 0 else door.queue.ahead(playerId),
            )
        scope.out.send(Lane.Now, Audience.Player(playerId), RoomQueueStatusOutCodec, status)
    }

    private fun notSpectator(
        scope: RoomScope,
        playerId: Int,
    ) = scope.out.send(
        Lane.Now,
        Audience.Player(playerId),
        YouAreNotSpectatorOutCodec,
        YouAreNotSpectatorOut(scope.room.info.id),
    )

    fun sessionOf(playerId: Int): Player? = environment.player(playerId)

    private fun dropOffline(scope: RoomScope) {
        val door = doorOf(scope.room)
        door.present
            .filter { (id, present) -> environment.player(id).let { it == null || it !== (present.session ?: it) } }
            .keys
            .forEach { remove(scope, it, toSelf = false) }
        val roomId = scope.room.info.id
        door.ringing.keys.filter { environment.player(it) == null }.forEach {
            door.ringing.remove(it)
            clearPending(it, roomId)
        }
        door.queue.all().filter { environment.player(it.playerId) == null }.forEach {
            door.queue.remove(it.playerId)
            clearPending(it.playerId, roomId)
        }
    }
}
