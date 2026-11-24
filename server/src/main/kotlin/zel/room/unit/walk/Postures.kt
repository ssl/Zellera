package zel.room.unit.walk

import zel.game.items.FurniHeights
import zel.room.Room
import zel.room.RoomScope
import zel.room.event.RoomEvent
import zel.room.furni.RoomFurni
import zel.room.map.Point
import zel.room.unit.RoomUnit
import zel.room.unit.UnitDirection
import zel.room.unit.UnitKind
import zel.room.unit.UnitStatus
import zel.room.unit.path.TileState
import zel.room.unit.path.WalkTiles
import zel.room.unit.pet.PetLines
import kotlin.math.abs

class Postures(
    private val hooks: WalkHooks,
) {
    fun settle(
        scope: RoomScope,
        unit: RoomUnit,
        walk: UnitWalk,
    ) {
        if (walk.rides != null) {
            unit.dropStatus(UnitStatus.SIT)
            unit.dropStatus(UnitStatus.LAY)
            unit.dropStatus(PetLines.INSIDE)
            return
        }
        val state = WalkTiles.at(scope.room, unit.tile).state
        when (state) {
            is TileState.Seat -> sit(scope, unit, walk, state.furni)
            is TileState.Bed -> lay(scope.room, scope, unit, walk, state.furni)
            else -> stand(unit, walk)
        }
        if (unit.kind is UnitKind.Pet) inside(scope.room, unit, state)
    }

    /**
     * A unit put down without walking there (a bot or pet at room load): on a seat it sits, on a bed it lies, as on
     * arrival, without the taken-posture hooks, which need a running room.
     */
    fun rest(
        room: Room,
        unit: RoomUnit,
        walk: UnitWalk,
    ) {
        if (walk.rides != null) return
        when (val state = WalkTiles.at(room, unit.tile).state) {
            is TileState.Seat -> sit(null, unit, walk, state.furni)
            is TileState.Bed -> lay(room, null, unit, walk, state.furni)
            else -> Unit
        }
    }

    /** A pet inside a furni that is not walked on: 0.0 under a furni others stack on, else how far its top is above. */
    private fun inside(
        room: Room,
        unit: RoomUnit,
        state: TileState,
    ) {
        val top =
            room.furni
                .at(unit.tile)
                .topItem()
                ?.takeIf { state == TileState.Blocked || state is TileState.Restricted }
        val value =
            when {
                top == null -> null
                top.definition.canStack -> 0f.toString()
                else -> (top.top - unit.z).takeIf { it > 0 }?.toFloat()?.toString()
            }
        if (value == null) unit.dropStatus(PetLines.INSIDE) else unit.setStatus(PetLines.INSIDE, value)
    }

    fun keep(
        scope: RoomScope,
        unit: RoomUnit,
        walk: UnitWalk,
    ) {
        val sitting = unit.showsStatus(UnitStatus.SIT) && !walk.floorSit
        val lying = unit.showsStatus(UnitStatus.LAY)
        if (!sitting && !lying) return
        val state = WalkTiles.at(scope.room, unit.tile).state
        when {
            sitting && state is TileState.Seat -> sit(scope, unit, walk, state.furni)
            lying && state is TileState.Bed -> lay(scope.room, scope, unit, walk, state.furni)
            else -> stand(unit, walk)
        }
    }

    fun change(
        scope: RoomScope,
        unit: RoomUnit,
        walk: UnitWalk,
        posture: Int,
    ): Boolean {
        hooks.active(scope, unit)
        if (unit.isWalking) stop(unit, walk)
        return when (posture) {
            SIT_DOWN -> floorSit(scope, unit, walk)
            STAND_UP -> standUp(scope, unit, walk)
            else -> false
        }
    }

    fun lookTo(
        unit: RoomUnit,
        point: Point,
    ) {
        val direction = lookDirection(unit, point) ?: return
        if (unit.showsStatus(UnitStatus.SIT)) turnHead(unit, direction) else unit.face(direction)
    }

    /**
     * The clicker of a unit turns to it: a point one step or less from the body's direction turns only the head,
     * one further round turns body and head; seated, only the head.
     */
    fun turnToClicked(
        unit: RoomUnit,
        point: Point,
    ) {
        val direction = lookDirection(unit, point) ?: return
        val headOnly = unit.showsStatus(UnitStatus.SIT) || inReach(unit, direction)
        if (headOnly) turnHead(unit, direction) else unit.face(direction)
    }

    private fun lookDirection(
        unit: RoomUnit,
        point: Point,
    ): Int? {
        val still = unit.isWalking || unit.showsStatus(UnitStatus.LAY) || hooks.asleep(unit) || !hooks.mayWalk(unit)
        return if (still) null else UnitDirection.between(unit.tile, point)
    }

    /** A seated head follows a point at most one step from the body's direction; further round it stays. */
    private fun turnHead(
        unit: RoomUnit,
        direction: Int,
    ) {
        if (inReach(unit, direction) && unit.headDirection != direction) {
            unit.headDirection = direction
            unit.changed = true
        }
    }

    private fun inReach(
        unit: RoomUnit,
        direction: Int,
    ): Boolean {
        val apart = Math.floorMod(direction - unit.bodyDirection, UnitDirection.COUNT)
        return apart <= 1 || apart == UnitDirection.COUNT - 1
    }

    private fun floorSit(
        scope: RoomScope,
        unit: RoomUnit,
        walk: UnitWalk,
    ): Boolean {
        if (unit.showsStatus(UnitStatus.SIT) || walk.rides != null || onWater(scope.room, unit.tile)) return false
        hooks.postureTaken(scope, unit)
        turnEven(unit)
        unit.dropStatus(UnitStatus.LAY)
        unit.setStatus(UnitStatus.SIT, FLOOR_SIT)
        walk.floorSit = true
        walk.seatFacing = null
        scope.raise(RoomEvent.Action(unit.index, ACTION_SIT, ""))
        return true
    }

    private fun standUp(
        scope: RoomScope,
        unit: RoomUnit,
        walk: UnitWalk,
    ): Boolean {
        if (onSeatBed(scope.room, unit)) return false
        val wasDown = unit.showsStatus(UnitStatus.SIT) || unit.showsStatus(UnitStatus.LAY)
        walk.floorSit = false
        stand(unit, walk)
        turnEven(unit)
        if (wasDown) scope.raise(RoomEvent.Action(unit.index, ACTION_STAND, ""))
        return unit.changed
    }

    /** A furni that is both seat and bed keeps its unit down; a plain chair or bed lets it stand up. */
    private fun onSeatBed(
        room: Room,
        unit: RoomUnit,
    ): Boolean =
        room.furni
            .at(unit.tile)
            .topItem()
            ?.definition
            ?.let { it.canSit && it.canLay } == true

    /**
     * Sits [unit] on [seat]. It faces the seat when it sits down and again when the seat itself turns (by hand or by
     * wired); [UnitWalk.seatFacing] remembers the seat direction it last faced, so a unit turned on its own while
     * seated (a rotate-user effect) is not turned back every tick. [scope] is null at room load (no hooks then).
     */
    private fun sit(
        scope: RoomScope?,
        unit: RoomUnit,
        walk: UnitWalk,
        seat: RoomFurni,
    ) {
        val newly = !unit.showsStatus(UnitStatus.SIT) || walk.floorSit
        walk.floorSit = false
        unit.dropStatus(UnitStatus.LAY)
        unit.setStatus(UnitStatus.SIT, "${FurniHeights.formatHeight(seat.height)} 0")
        moveZ(unit, seat.z)
        if (newly || walk.seatFacing != seat.direction) unit.face(seat.direction)
        walk.seatFacing = seat.direction
        if (newly && scope != null) {
            hooks.postureTaken(scope, unit)
            scope.raise(RoomEvent.Action(unit.index, ACTION_SIT, ""))
        }
    }

    private fun lay(
        room: Room,
        scope: RoomScope?,
        unit: RoomUnit,
        walk: UnitWalk,
        bed: RoomFurni,
    ) {
        val pillow = Beds.pillowFor(bed, unit.tile)
        if (pillow != unit.tile && pillow in bed.tiles() && !holdsOther(room, pillow, unit)) {
            unit.tile = pillow
            unit.changed = true
        }
        val newly = !unit.showsStatus(UnitStatus.LAY)
        walk.floorSit = false
        unit.dropStatus(UnitStatus.SIT)
        val params = bed.definition.params.ifBlank { "null" }
        unit.setStatus(UnitStatus.LAY, "${FurniHeights.formatHeight(bed.height)} $params")
        moveZ(unit, bed.z)
        val facing = Math.floorMod(bed.direction, BED_DIRECTIONS)
        if (newly || walk.seatFacing != facing) unit.face(facing)
        walk.seatFacing = facing
        if (newly && scope != null) {
            hooks.postureTaken(scope, unit)
            scope.raise(RoomEvent.Action(unit.index, ACTION_LAY, ""))
        }
    }

    private fun stand(
        unit: RoomUnit,
        walk: UnitWalk,
    ) {
        if (!walk.floorSit) unit.dropStatus(UnitStatus.SIT)
        unit.dropStatus(UnitStatus.LAY)
        walk.seatFacing = null
    }

    private fun stop(
        unit: RoomUnit,
        walk: UnitWalk,
    ) {
        unit.path = emptyList()
        unit.dropStatus(UnitStatus.MOVE)
        walk.shownTile = null
    }

    private fun turnEven(unit: RoomUnit) {
        val even = unit.bodyDirection - Math.floorMod(unit.bodyDirection, 2)
        if (even != unit.bodyDirection) {
            unit.bodyDirection = even
            unit.changed = true
        }
    }

    private fun moveZ(
        unit: RoomUnit,
        z: Double,
    ) {
        if (abs(unit.z - z) > HEIGHT_EPSILON) {
            unit.z = z
            unit.changed = true
        }
    }

    private fun onWater(
        room: Room,
        tile: Point,
    ): Boolean =
        room.furni
            .at(tile)
            .topItem()
            ?.definition
            ?.interaction
            ?.name
            ?.lowercase() in WATER

    private fun holdsOther(
        room: Room,
        tile: Point,
        unit: RoomUnit,
    ): Boolean = room.units.all().any { it !== unit && it.tile == tile }

    companion object {
        const val SIT_DOWN = 1
        const val STAND_UP = 0

        const val ACTION_SIT = 6
        const val ACTION_STAND = 7
        const val ACTION_LAY = 8

        const val FLOOR_SIT = "0.55 1"

        private const val HEIGHT_EPSILON = 1e-6
        private const val BED_DIRECTIONS = 4
        private val WATER = setOf("water.area", "water.float")
    }
}
