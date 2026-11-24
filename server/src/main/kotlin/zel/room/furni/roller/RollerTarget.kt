package zel.room.furni.roller

import zel.room.Room
import zel.room.furni.RoomFurni
import zel.room.furni.TileStack
import zel.room.furni.place.Interactions
import zel.room.map.Point
import kotlin.math.abs

data class RollerTarget(
    val tile: Point,
    val open: Boolean,
    /** A roller that already carries something takes units but no more furni. */
    val takesFurni: Boolean = open,
) {
    companion object {
        private const val GATE = "gate.plain"

        fun of(
            room: Room,
            roller: RoomFurni,
            heightTolerance: Double = Rollers.DEFAULT_HEIGHT_TOLERANCE,
        ): RollerTarget {
            val from = requireNotNull(roller.anchor) { "roller ${roller.id} is not on the floor" }
            val tile = from.step(roller.direction)
            val stack = room.furni.at(tile)
            val targetRoller = stack.furni.firstOrNull(::isRoller)
            val blocked =
                room.floor.baseHeight(tile) == null ||
                    takesNothing(stack) ||
                    room.units.all().any { it.tile == tile } ||
                    (targetRoller != null && !sameRelativeHeight(room, roller, targetRoller, heightTolerance))
            val crowded = targetRoller != null && stack.furni.size > 1
            return RollerTarget(tile, open = !blocked, takesFurni = !blocked && !crowded)
        }

        fun isRoller(furni: RoomFurni): Boolean = Interactions.of(furni.definition) == Interactions.ROLLER

        private fun takesNothing(stack: TileStack): Boolean {
            if (stack.helperZ() != null) return false
            val top = stack.topItem() ?: return false
            if (Interactions.of(top.definition) == GATE) return (top.state.trim().toIntOrNull() ?: 0) == 0
            val definition = top.definition
            return !top.isStackable && !definition.canWalk && !definition.canSit && !definition.canLay
        }

        private fun sameRelativeHeight(
            room: Room,
            roller: RoomFurni,
            target: RoomFurni,
            tolerance: Double,
        ): Boolean = abs(aboveFloor(room, roller) - aboveFloor(room, target)) <= tolerance

        private fun aboveFloor(
            room: Room,
            furni: RoomFurni,
        ): Double = furni.z - (furni.anchor?.let { room.floor.floorZ(it) } ?: 0.0)
    }
}
