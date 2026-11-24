package zel.wired.condition

import zel.room.unit.RoomUnit
import zel.room.unit.UnitKind
import zel.wired.box.BoxSettings
import zel.wired.box.ConditionBox
import zel.wired.firing.Firing
import zel.wired.firing.Targets
import zel.wired.firing.UserSource

class TriggererMatch(
    override val furniId: Int,
    override val settings: BoxSettings,
    private val facts: UnitFacts,
    private val negative: Boolean,
) : ConditionBox {
    override fun passes(firing: Firing): Boolean {
        val checked = Targets.units(firing, this, CHECKED)
        if (checked.isEmpty()) return Quantified.noUsers(this, negative, CHECKED)
        val kinds = settings.intAt(0)
        val compare = compare(firing)
        val positive =
            Quantified.holds(checked, settings) { unit -> kindBit(unit) and kinds != 0 && compare.any { it === unit } }
        return positive != negative
    }

    /** An empty name is no name filter, also for "by name" (101): then every unit of the room is compared. */
    private fun compare(firing: Firing): List<RoomUnit> {
        val name = settings.text
        val candidates =
            if (settings.userSources.getOrNull(COMPARE) == UserSource.BY_NAME_SECOND) {
                firing.scope.room.units
                    .all()
            } else {
                Targets.units(firing, this, COMPARE)
            }
        return candidates.filter { name.isEmpty() || facts.name(firing.scope, it).equals(name, ignoreCase = true) }
    }

    private fun kindBit(unit: RoomUnit): Int =
        when (unit.kind) {
            is UnitKind.Player -> USER
            is UnitKind.Pet -> PET
            is UnitKind.Bot -> BOT
        }

    private companion object {
        const val CHECKED = 0
        const val COMPARE = 1
        const val USER = 1
        const val PET = 2
        const val BOT = 4
    }
}
