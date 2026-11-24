package zel.room.unit.walk

import zel.game.items.FurniHeights
import zel.protocol.generated.RoomUnitUpdateOut
import zel.room.unit.RoomUnit
import zel.room.unit.UnitKind
import zel.room.unit.UnitStatus
import zel.room.unit.pet.PetLines

object UnitLines {
    fun status(
        unit: RoomUnit,
        emptyMoveSlot: Boolean = false,
    ): String {
        val parts = unit.statuses.entries.map { (key, value) -> if (value.isEmpty()) key else "$key $value" }
        val posture = unit.showsStatus(UnitStatus.SIT) || unit.showsStatus(UnitStatus.LAY)
        val withSlot =
            if (!emptyMoveSlot) {
                parts
            } else {
                val at = unit.statuses.keys.indexOfFirst { it == UnitStatus.SIT || it == UnitStatus.LAY }
                parts.toMutableList().apply { add(if (at < 0) size else at, "") }
            }
        val text = withSlot.joinToString(separator = "", prefix = "/") { "$it/" }
        return if (posture) text else "$text/"
    }

    fun line(
        unit: RoomUnit,
        walk: UnitWalk?,
    ): RoomUnitUpdateOut.List1Item {
        val tile = walk?.shownTile ?: unit.tile
        val z = if (walk?.shownTile != null) walk.shownZ else unit.z
        return RoomUnitUpdateOut.List1Item(
            int1 = unit.index,
            int2 = tile.x,
            int3 = tile.y,
            string1 = FurniHeights.formatZ(z),
            int4 = unit.headDirection,
            int5 = unit.bodyDirection,
            int6 = 0,
            string2 =
                if (unit.kind is UnitKind.Pet) {
                    PetLines.status(unit)
                } else {
                    status(unit, emptyMoveSlot = walk?.stayLines == STAY_WITH_SLOT)
                },
        )
    }

    fun stepLine(
        unit: RoomUnit,
        step: StepOrigin,
    ): RoomUnitUpdateOut.List1Item {
        val parts =
            unit.statuses.entries.filter { it.key != UnitStatus.MOVE }.map { (key, value) ->
                if (value.isEmpty()) key else "$key $value"
            }
        val move = "${UnitStatus.MOVE} ${step.to.x},${step.to.y},${FurniHeights.formatZ(step.toZ)}"
        return RoomUnitUpdateOut.List1Item(
            int1 = unit.index,
            int2 = step.tile.x,
            int3 = step.tile.y,
            string1 = FurniHeights.formatZ(step.z),
            int4 = unit.headDirection,
            int5 = unit.bodyDirection,
            int6 = 0,
            string2 = (parts + move).joinToString(separator = "", prefix = "/", postfix = "/") { "$it/" },
        )
    }

    const val STAY_WITH_SLOT = 2
}
