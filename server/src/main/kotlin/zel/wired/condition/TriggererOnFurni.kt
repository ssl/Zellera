package zel.wired.condition

import zel.wired.box.BoxSettings
import zel.wired.box.ConditionBox
import zel.wired.firing.Firing
import zel.wired.firing.Targets

class TriggererOnFurni(
    override val furniId: Int,
    override val settings: BoxSettings,
    private val negative: Boolean,
) : ConditionBox {
    override fun passes(firing: Firing): Boolean {
        val users = Targets.units(firing, this)
        if (users.isEmpty()) return Quantified.noUsers(this, negative)
        val furni = Targets.furni(firing, this).map { it.id }.toSet()
        val room = firing.scope.room
        val positive =
            Quantified.holds(users, settings) { unit ->
                room.furni
                    .at(unit.tile)
                    .furni
                    .any { it.id in furni }
            }
        return positive != negative
    }
}
