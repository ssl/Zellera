package zel.wired.condition

import zel.wired.box.BoxSettings
import zel.wired.box.ConditionBox
import zel.wired.firing.Firing
import zel.wired.firing.Targets

class HasHandItem(
    override val furniId: Int,
    override val settings: BoxSettings,
    private val facts: UnitFacts,
    private val negative: Boolean,
) : ConditionBox {
    override fun passes(firing: Firing): Boolean {
        val users = Targets.units(firing, this)
        if (users.isEmpty()) return Quantified.noUsers(this, negative)
        val item = settings.intAt(0).coerceAtLeast(0)
        return Quantified.holds(users, settings) { facts.handItem(it) == item } != negative
    }
}
