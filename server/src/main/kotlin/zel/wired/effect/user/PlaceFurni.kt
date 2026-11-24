package zel.wired.effect.user

import zel.protocol.generated.ObjectsOutCodec
import zel.room.RoomScope
import zel.room.entry.EntryMessages
import zel.room.furni.HeightMapChange
import zel.room.furni.RoomFurni
import zel.room.furni.place.FloorLanding
import zel.room.furni.place.FurniOut
import zel.room.furni.place.Interactions
import zel.room.furni.place.RoomLimits
import zel.room.furni.place.UnitsOnTiles
import zel.room.map.Point
import zel.room.tick.Audience
import zel.room.tick.Lane
import zel.wired.WiredRooms
import zel.wired.box.BoxSettings
import zel.wired.box.EffectBox
import zel.wired.firing.Firing
import zel.wired.firing.Targets
import zel.wired.usage.WiredError
import zel.wired.variable.ChangeOrigin
import zel.wired.variable.Holder
import zel.wired.variable.RoomVariables
import zel.wired.variable.VariableOn

class PlaceFurni(
    override val furniId: Int,
    override val settings: BoxSettings,
    private val state: UserEffectState,
    private val host: UserEffectHost,
    private val units: UnitsOnTiles,
    private val variables: RoomVariables? = null,
    private val limits: RoomLimits = RoomLimits(),
    private val wired: () -> WiredRooms? = { null },
) : EffectBox {
    override val negative = false
    override val needsUser = false

    override fun run(firing: Firing) {
        val sources =
            Targets.furni(firing, this, SOURCE_SLOT).filter { it.anchor != null && CloneSources.allows(it) }
        if (sources.isEmpty()) return
        val corner = Point(sources.minOf { requireNotNull(it.anchor).x }, sources.minOf { requireNotNull(it.anchor).y })
        val custom = at(LOCATION) == CUSTOM_LOCATION
        val spot = if (custom) customSpot(firing) ?: return else null
        sources.forEach { source ->
            val anchor = requireNotNull(source.anchor)
            val base = spot?.let { Point(it.x + anchor.x - corner.x, it.y + anchor.y - corner.y) } ?: anchor
            place(firing, source, Point(base.x + at(OFFSET_X), base.y + at(OFFSET_Y)))
        }
    }

    private fun customSpot(firing: Firing): Point? =
        if (at(TARGET_IS_USER) != 0) {
            Targets.units(firing, this, USER_TARGET_SLOT).firstOrNull()?.tile
        } else {
            Targets.furni(firing, this, FURNI_TARGET_SLOT).firstNotNullOfOrNull { it.anchor }
        }

    private fun place(
        firing: Firing,
        source: RoomFurni,
        tile: Point,
    ) {
        val scope = firing.scope
        val room = scope.room
        if (limits.check(room, source.definition) != null) return
        val landing = FloorLanding.place(room, source.definition, tile, source.direction)
        if (landing !is FloorLanding.Result.Lands) return
        val kept = state.of(room)
        if (kept.copies.size >= CopyAllowance.MAX_LIVE || !kept.copyAllowance.take(scope.now())) {
            failed(scope)
            return
        }
        val z = altitude(source, landing.z)
        val place = CopyPlace(room.info.id, tile.x, tile.y, z, source.direction)
        val id = host.placeCopy(room.info.ownerId, source.definition, source.state, place)
        if (id <= 0) {
            failed(scope)
            return
        }
        val heights = HeightMapChange.of(room, landing.tiles)
        val copy =
            RoomFurni(
                id = id,
                ownerId = room.info.ownerId,
                ownerName = room.info.ownerName,
                definition = source.definition,
                behaviour = host.behaviour(Interactions.of(source.definition)),
                anchor = tile,
                z = z,
                direction = source.direction,
                state = source.state,
                wallPosition = null,
                groupId = 0,
                limitedData = NOT_LIMITED,
                wiredCopy = true,
            )
        room.furni.add(copy)
        kept.copies += id
        scope.out.send(Lane.Now, Audience.Room, ObjectsOutCodec, EntryMessages.objects(listOf(copy)))
        FurniOut.heights(scope, heights)
        units.furniChanged(scope, emptyList(), landing.tiles)
        spawnVariable(firing, copy)
    }

    private fun failed(scope: RoomScope) {
        wired()?.raise(scope, WiredError.PLACEMENT_FAILURE)
    }

    private fun spawnVariable(
        firing: Firing,
        copy: RoomFurni,
    ) {
        if (at(WITH_VARIABLE) == 0 || variables == null) return
        val variable =
            variables.find(firing.scope.room, settings.variableIds.firstOrNull().orEmpty()) ?: return
        val value =
            if (at(VALUE_OPTION) == VALUE_FROM_VARIABLE) {
                variables.readFor(
                    firing,
                    this,
                    variableSlot = 1,
                    target = at(REFERENCE_TARGET),
                    furniSlot = REFERENCE_FURNI_SLOT,
                    userSlot = REFERENCE_USER_SLOT,
                ) ?: return
            } else {
                at(VALUE).toLong()
            }
        variable.give(VariableOn(firing.scope, firing), Holder.of(copy), value, true, ChangeOrigin.IN_ROOM)
    }

    private fun altitude(
        source: RoomFurni,
        onTop: Double,
    ): Double {
        val z =
            when (at(ALTITUDE)) {
                ALTITUDE_SOURCE -> source.z
                ALTITUDE_FLOOR -> 0.0
                else -> onTop
            }
        return (z + at(OFFSET_ALTITUDE) / HUNDREDTHS).coerceIn(0.0, FloorLanding.WIRED_CEILING)
    }

    private fun at(index: Int): Int = settings.ints.getOrNull(index) ?: 0

    private companion object {
        const val TARGET_IS_USER = 0
        const val LOCATION = 1
        const val ALTITUDE = 2
        const val OFFSET_X = 3
        const val OFFSET_Y = 4
        const val OFFSET_ALTITUDE = 5
        const val WITH_VARIABLE = 6
        const val VALUE_OPTION = 7
        const val VALUE = 8
        const val REFERENCE_TARGET = 9
        const val VALUE_FROM_VARIABLE = 1
        const val REFERENCE_FURNI_SLOT = 2
        const val REFERENCE_USER_SLOT = 1
        const val CUSTOM_LOCATION = 1
        const val ALTITUDE_SOURCE = 1
        const val ALTITUDE_FLOOR = 2
        const val SOURCE_SLOT = 0
        const val FURNI_TARGET_SLOT = 1
        const val USER_TARGET_SLOT = 0
        const val HUNDREDTHS = 100.0
        const val NOT_LIMITED = "0:0"
    }
}
