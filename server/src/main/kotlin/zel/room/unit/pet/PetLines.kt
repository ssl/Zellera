package zel.room.unit.pet

import zel.game.pets.MonsterPlant
import zel.game.pets.Pet
import zel.room.unit.RoomUnit
import zel.room.unit.UnitStatus

object PetLines {
    const val POSTURE = "~posture"

    const val GESTURE = "gst"

    /** Gesture values shown under [GESTURE]. */
    const val SMILE = "sml"

    const val JOY = "joy"

    const val REBRED = "reb"

    const val STAND = "std"

    /** How far above the pet the furni it stands inside reaches, as its posture carries it. */
    const val INSIDE = "~inside"

    private const val SEAT_DROP = 0.55f

    private const val BED_DROP = 1.0f

    private const val NO_PARAMS = "null"

    fun restingPosture(pet: Pet): String = (pet as? MonsterPlant)?.let(PlantLife::restingPosture) ?: STAND

    /** The status line; on a seat or bed the first slot carries the pet's lower height, inside a furni [INSIDE]. */
    fun status(unit: RoomUnit): String {
        val move = unit.statuses[UnitStatus.MOVE]
        val slot = slot(unit)
        val height = slot?.height ?: unit.statuses[INSIDE]?.takeIf { move == null }
        val posture = unit.statuses[POSTURE]?.let { if (height != null) "$it $height" else it }
        val rest = listOfNotNull(posture, unit.statuses[GESTURE]?.let { "$GESTURE $it" }).joinToString("/")
        return when {
            move != null -> "${UnitStatus.MOVE} $move///$rest"
            slot != null -> "${slot.key} ${slot.height} ${slot.tail}//$rest"
            else -> "//$rest"
        }
    }

    private class Slot(
        val key: String,
        val height: String,
        val tail: String,
    )

    private fun slot(unit: RoomUnit): Slot? {
        unit.statuses[UnitStatus.SIT]?.let { seat ->
            val height = lowered(seat, SEAT_DROP) ?: return@let
            return Slot(UnitStatus.SIT, height, seat.substringAfter(' ', "0"))
        }
        val bed = unit.statuses[UnitStatus.LAY] ?: return null
        val height = lowered(bed, BED_DROP) ?: return null
        return Slot(UnitStatus.LAY, height, bed.substringAfter(' ', NO_PARAMS).ifBlank { NO_PARAMS })
    }

    private fun lowered(
        value: String,
        drop: Float,
    ): String? = value.substringBefore(' ').toFloatOrNull()?.let { (it - drop).toString() }
}
