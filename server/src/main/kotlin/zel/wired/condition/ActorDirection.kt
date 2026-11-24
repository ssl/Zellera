package zel.wired.condition

import zel.room.unit.UnitDirection
import zel.wired.box.BoxSettings
import zel.wired.box.ConditionBox
import zel.wired.firing.Firing
import zel.wired.firing.Targets

class ActorDirection(
    override val furniId: Int,
    override val settings: BoxSettings,
) : ConditionBox {
    override fun passes(firing: Firing): Boolean {
        val mask = settings.intAt(0)
        val users = Targets.units(firing, this)
        if (users.isEmpty()) return Quantified.noUsers(this)
        return Quantified.holds(users, settings) {
            it.bodyDirection in 0 until UnitDirection.COUNT && (mask shr it.bodyDirection) and 1 != 0
        }
    }
}
