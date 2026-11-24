package zel.room.furni.roller

import zel.protocol.generated.SlideObjectBundleOut
import zel.room.furni.RoomFurni
import zel.room.map.Point
import zel.room.unit.RoomUnit
import zel.room.unit.UnitStatus
import zel.room.unit.path.TileState
import zel.room.unit.path.WalkTiles

class RollingUnits(
    private val maxUnits: Int,
) {
    /** Moves the units the roller takes this pass; their slides are for the caller to send, in this order. */
    fun roll(
        pass: RollerPass,
        roller: RoomFurni,
        target: RollerTarget,
    ): List<SlideObjectBundleOut> {
        if (!target.open) return emptyList()
        val room = pass.scope.room
        val from = roller.anchor ?: return emptyList()
        val newTop = room.furni.at(target.tile).topItem()
        // A stack helper's tile takes no unit, unless the helper lifts a roller.
        if (room.furni.at(target.tile).helperZ() != null && (newTop == null || !RollerTarget.isRoller(newTop))) {
            return emptyList()
        }
        val state = WalkTiles.at(room, target.tile).state
        if (state != TileState.Open && state !is TileState.Seat) return emptyList()
        val leftTop = room.furni.at(from).topItem()
        val slides = ArrayList<SlideObjectBundleOut>()
        room.units
            .all()
            .filter { it.tile == from && mayRoll(pass, it) }
            .take(maxUnits)
            .forEach { unit ->
                pass.walking.horseOf(pass.scope, unit)?.let { slides += slide(pass, it, roller, from, target.tile) }
                slides += slide(pass, unit, roller, from, target.tile)
                // The arrival line and the walk triggers come with the next tick.
                val due = pass.room.ticks + 1
                pass.room.owed += RolledStep(unit.index, target.tile, leftTop?.id, newTop?.id, due)
            }
        return slides
    }

    private fun mayRoll(
        pass: RollerPass,
        unit: RoomUnit,
    ): Boolean {
        if (unit.index in pass.movedUnits || unit.isWalking || unit.showsStatus(UnitStatus.MOVE)) return false
        return !pass.walking.isRidden(pass.scope, unit)
    }

    private fun slide(
        pass: RollerPass,
        unit: RoomUnit,
        roller: RoomFurni,
        from: Point,
        to: Point,
    ): SlideObjectBundleOut {
        val scope = pass.scope
        val fromZ = unit.z
        val before = LinkedHashMap(unit.statuses)
        pass.walking.place(scope, unit, to)
        unit.changed = unit.statuses != before
        pass.walking.movedByWired(scope, unit)
        pass.movedUnits += unit.index
        return RollerSlides.unit(from, to, roller.id, unit.index, fromZ, unit.z)
    }
}
