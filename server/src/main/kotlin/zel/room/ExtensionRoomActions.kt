package zel.room

import zel.ext.ExtensionRooms
import zel.ext.Moves
import zel.ext.Scenery
import zel.game.bots.Bot
import zel.game.bots.BotSpeech
import zel.game.items.FurniHeights
import zel.game.items.Item
import zel.game.players.Gender
import zel.protocol.generated.NotificationDialogOut
import zel.protocol.generated.NotificationDialogOutCodec
import zel.protocol.generated.ObjectRemoveMultipleOut
import zel.protocol.generated.ObjectRemoveMultipleOutCodec
import zel.protocol.generated.ObjectsDataUpdateOut
import zel.protocol.generated.ObjectsDataUpdateOutCodec
import zel.protocol.generated.ObjectsOut
import zel.protocol.generated.ObjectsOutCodec
import zel.protocol.generated.StuffData
import zel.protocol.generated.WiredMovementsOutCodec
import zel.room.chat.ChatKind
import zel.room.entry.EntryMessages
import zel.room.furni.FurniBehaviour
import zel.room.furni.HeightMapChange
import zel.room.furni.RoomFurni
import zel.room.furni.place.FurniOut
import zel.room.furni.place.FurniWire
import zel.room.furni.place.Interactions
import zel.room.map.FloorPlan
import zel.room.map.Footprint
import zel.room.map.Point
import zel.room.tick.Audience
import zel.room.tick.Lane
import zel.room.tick.RoomTiming
import zel.room.unit.RoomUnit
import zel.room.unit.UnitKind
import zel.room.unit.UnitStatus
import zel.room.unit.bot.RoomBots
import zel.room.unit.bot.UnitSpeech
import zel.room.unit.path.PathResult
import zel.room.unit.walk.Walking
import zel.wired.firing.Movement
import zel.wired.firing.MovementWire
import zel.wired.timing.WIRED_MOVE
import zel.wired.timing.postureHeight
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

@Suppress("TooManyFunctions") // one per action an extension may take
internal class ExtensionRoomActions(
    private val registry: () -> RoomRegistry?,
    private val walking: Walking,
    private val definitions: (String) -> Item?,
    private val behaviourOf: (String) -> FurniBehaviour,
    private val names: (scope: RoomScope, unit: RoomUnit) -> String?,
    private val travels: (scope: RoomScope, playerId: Int, roomId: Int) -> Boolean,
    private val bots: () -> RoomBots?,
) : ExtensionRooms {
    private val nextId = AtomicInteger(FIRST_ID)

    private val lastSlides = ConcurrentHashMap<RoomUnit, Long>()

    override fun roomOf(playerId: Int): Int? = registry()?.roomOf(playerId)

    override fun post(
        roomId: Int,
        delayMillis: Long,
        block: (RoomScope) -> Unit,
    ): Boolean = registry()?.post(roomId, delayMillis, block) == true

    override fun nameOf(
        scope: RoomScope,
        unit: RoomUnit,
    ): String? = names(scope, unit)

    override fun definition(code: String): Item? = definitions(code)

    override fun addFurni(
        scope: RoomScope,
        definition: Item,
        tile: Point,
        z: Double,
        direction: Int,
        state: String,
    ): RoomFurni {
        val room = scope.room
        val furni =
            RoomFurni(
                id = nextId.getAndUpdate { if (it + 1 >= LAST_ID) FIRST_ID else it + 1 },
                ownerId = room.info.ownerId,
                ownerName = room.info.ownerName,
                definition = definition,
                behaviour = behaviourOf(Interactions.of(definition)),
                anchor = tile,
                z = z,
                direction = direction,
                state = state,
                wallPosition = null,
                groupId = 0,
                limitedData = NOT_LIMITED,
                wiredCopy = true,
            )
        val heights = HeightMapChange.of(room, furni.tiles())
        room.furni.add(furni)
        scope.out.send(Lane.Now, Audience.Room, ObjectsOutCodec, EntryMessages.objects(listOf(furni)))
        FurniOut.heights(scope, heights)
        return furni
    }

    override fun removeFurni(
        scope: RoomScope,
        furni: RoomFurni,
    ) {
        val room = scope.room
        if (room.furni[furni.id] !== furni) return
        val heights = HeightMapChange.of(room, furni.tiles())
        room.furni.remove(furni.id)
        scope.out.send(
            Lane.Now,
            Audience.Room,
            ObjectRemoveMultipleOutCodec,
            ObjectRemoveMultipleOut(listOf(furni.id), NO_PICKER, false),
        )
        FurniOut.heights(scope, heights)
    }

    override fun setState(
        scope: RoomScope,
        furni: RoomFurni,
        state: String,
    ) {
        if (furni.state == state) return
        furni.state = state
        val item = ObjectsDataUpdateOut.List1Item(furni.id, FurniWire.stuff(furni))
        scope.out.send(Lane.Now, Audience.Room, ObjectsDataUpdateOutCodec, ObjectsDataUpdateOut(listOf(item)))
    }

    override fun moves(scope: RoomScope): Moves = Batch(scope)

    override fun place(
        scope: RoomScope,
        unit: RoomUnit,
        tile: Point,
        z: Double?,
    ) {
        walking.unseat(scope, unit)
        unit.dropStatus(UnitStatus.LAY)
        unit.dropStatus(WIRED_MOVE)
        walking.place(scope, unit, tile, z)
    }

    override fun sit(
        scope: RoomScope,
        unit: RoomUnit,
        height: Double,
        direction: Int,
    ) = walking.seat(scope, unit, height, direction)

    override fun lie(
        scope: RoomScope,
        unit: RoomUnit,
        direction: Int,
    ) {
        unit.path = emptyList()
        unit.dropStatus(UnitStatus.MOVE)
        unit.dropStatus(UnitStatus.SIT)
        unit.face(direction)
        unit.setStatus(UnitStatus.LAY, LAY_HEIGHT)
        unit.changed = true
    }

    override fun stand(
        scope: RoomScope,
        unit: RoomUnit,
    ) {
        walking.unseat(scope, unit)
        unit.dropStatus(UnitStatus.LAY)
    }

    override fun addBot(
        scope: RoomScope,
        name: String,
        figure: String,
        female: Boolean,
        tile: Point,
        direction: Int,
        motto: String,
    ): RoomUnit {
        val room = scope.room
        val bot =
            Bot(
                RoomBots.passerById(),
                PASSER_BY_KIND,
                name,
                motto,
                figure,
                if (female) Gender.F else Gender.M,
                room.info.ownerId,
                room.info.ownerName,
                false,
                BotSpeech(emptyList(), false, false, SILENT_SECONDS),
                0,
                0,
            )
        return checkNotNull(bots()) { "no bots in this engine" }.addPasserBy(scope, bot, tile, direction)
    }

    override fun removeBot(
        scope: RoomScope,
        unit: RoomUnit,
    ) {
        bots()?.removePasserBy(scope, unit.index)
    }

    override fun walkTo(
        scope: RoomScope,
        unit: RoomUnit,
        tile: Point,
    ): Boolean = walking.walkTo(scope, unit, tile) is PathResult.Found

    override fun closeTiles(
        scope: RoomScope,
        tiles: Collection<Point>,
        closed: Boolean,
    ) {
        if (closed) scope.room.closedTiles += tiles else scope.room.closedTiles -= tiles.toSet()
    }

    override fun userClicks(
        room: Room,
        on: Boolean,
    ) {
        room.hearsUserClicks = on
    }

    override fun viewDistance(
        room: Room,
        tiles: Int,
    ) {
        room.sight.distance = tiles.coerceAtLeast(0)
    }

    override fun viewDistance(
        scope: RoomScope,
        playerId: Int,
        tiles: Int,
    ) {
        if (scope.room.sight.active) scope.room.sight.reach(playerId, tiles.coerceAtLeast(1))
    }

    override fun floor(
        roomId: Int,
        plan: () -> FloorPlan?,
    ) {
        registry()?.supplyFloor(roomId) { plan() }
    }

    override fun showScenery(
        scope: RoomScope,
        playerId: Int,
        items: List<Scenery>,
    ) {
        for (part in items.chunked(SCENERY_PER_MESSAGE)) {
            val message = ObjectsOut(emptyList(), part.map(::sceneryItem))
            scope.out.send(Lane.Now, Audience.Player(playerId), ObjectsOutCodec, message)
        }
    }

    override fun hideScenery(
        scope: RoomScope,
        playerId: Int,
        ids: List<Int>,
    ) {
        for (part in ids.chunked(SCENERY_PER_MESSAGE)) {
            val message = ObjectRemoveMultipleOut(part, NO_PICKER, true)
            scope.out.send(Lane.Now, Audience.Player(playerId), ObjectRemoveMultipleOutCodec, message)
        }
    }

    override fun say(
        scope: RoomScope,
        speaker: RoomUnit,
        text: String,
        shout: Boolean,
    ) = UnitSpeech.toRoom(scope, speaker, if (shout) ChatKind.Shout else ChatKind.Talk, text, 0)

    override fun whisper(
        scope: RoomScope,
        speaker: RoomUnit,
        to: RoomUnit,
        text: String,
    ) = UnitSpeech.whisper(scope, speaker, to, text, 0)

    override fun notify(
        scope: RoomScope,
        playerId: Int,
        type: String,
        params: Map<String, String>,
    ) {
        val message = NotificationDialogOut(type, params.map { (k, v) -> NotificationDialogOut.List1Item(k, v) })
        scope.out.send(Lane.Now, Audience.Player(playerId), NotificationDialogOutCodec, message)
    }

    override fun travel(
        scope: RoomScope,
        unit: RoomUnit,
        roomId: Int,
    ): Boolean {
        val playerId = (unit.kind as? UnitKind.Player)?.playerId ?: return false
        return travels(scope, playerId, roomId)
    }

    private inner class Batch(
        private val scope: RoomScope,
    ) : Moves {
        private val entries = mutableListOf<Movement>()
        private val heightTiles = mutableListOf<Point>()
        private val placed = mutableListOf<() -> Unit>()

        override fun furni(
            furni: RoomFurni,
            tile: Point,
            z: Double,
            direction: Int,
            millis: Int,
        ): Moves {
            val from = furni.anchor ?: return this
            val fromZ = furni.z
            heightTiles += furni.tiles()
            heightTiles += Footprint.tiles(tile, furni.definition.width, furni.definition.length, direction)
            placed += {
                if (scope.room.furni[furni.id] === furni) {
                    scope.room.furni.move(furni, tile, direction)
                    furni.z = z
                }
            }
            entries +=
                Movement.FurniMove(
                    furni.id,
                    Movement.Spot(from.x, from.y, FurniHeights.formatZ(fromZ)),
                    Movement.Spot(tile.x, tile.y, FurniHeights.formatZ(z)),
                    direction,
                    millis,
                )
            return this
        }

        override fun unit(
            unit: RoomUnit,
            tile: Point,
            z: Double,
            direction: Int,
            millis: Int,
        ): Moves {
            val posture = unit.postureHeight
            entries +=
                Movement.UnitMove(
                    unit.index,
                    Movement.Spot(unit.tile.x, unit.tile.y, FurniHeights.formatZ(unit.z + posture)),
                    Movement.Spot(tile.x, tile.y, FurniHeights.formatZ(z + posture)),
                    Movement.UnitMove.SLIDE,
                    millis,
                    direction,
                    direction,
                )
            placed += {
                unit.path = emptyList()
                unit.tile = tile
                unit.z = z
                unit.face(direction)
                sliding(unit)
            }
            return this
        }

        override fun send() {
            if (entries.isEmpty()) return
            val heights = HeightMapChange.of(scope.room, heightTiles.toList())
            placed.forEach { it() }
            scope.out.send(Lane.Now, Audience.Room, WiredMovementsOutCodec, MovementWire.message(entries))
            FurniOut.heights(scope, heights)
            entries.clear()
            placed.clear()
            heightTiles.clear()
        }

        /** Slide status before the posture keeps the unit seated and not walking. */
        private fun sliding(unit: RoomUnit) {
            val postures =
                listOf(UnitStatus.SIT, UnitStatus.LAY).mapNotNull { key ->
                    unit.statuses[key]?.let {
                        key to
                            it
                    }
                }
            postures.forEach { (key, _) -> unit.dropStatus(key) }
            unit.dropStatus(WIRED_MOVE)
            unit.setStatus(WIRED_MOVE)
            postures.forEach { (key, value) -> unit.setStatus(key, value) }
            unit.changed = true
            val at = scope.now()
            lastSlides[unit] = at
            val room = scope.room
            registry()?.post(room.info.id, RoomTiming.TICK_MILLIS) { later ->
                if (later.room === room && lastSlides[unit] == at) {
                    lastSlides.remove(unit)
                    if (room.units[unit.index] === unit) {
                        unit.dropStatus(WIRED_MOVE)
                        unit.changed = true
                    }
                }
            }
        }
    }

    companion object {
        /** Below the build pass ids, far above any saved furni. */
        const val FIRST_ID = 0x7F000000
        private const val LAST_ID = 0x7FFE0000
        private const val NOT_LIMITED = "0:0"
        private const val NO_PICKER = -1
        private const val SCENERY_PER_MESSAGE = 400
        private const val LAY_HEIGHT = "0.5 0"
        private const val PASSER_BY_KIND = "generic"
        private const val SILENT_SECONDS: Short = 30
    }
}

private const val NEVER_EXPIRES = -1

private fun sceneryItem(item: Scenery): ObjectsOut.List2Item =
    ObjectsOut.List2Item(
        item.id,
        item.spriteId,
        item.x,
        item.y,
        item.direction,
        FurniHeights.formatZ(item.z),
        "0",
        0,
        StuffData(0, StuffData.Value.Legacy(item.state.toString())),
        NEVER_EXPIRES,
        0,
        0,
        ObjectsOut.List2Item.Branch1.Else,
    )
