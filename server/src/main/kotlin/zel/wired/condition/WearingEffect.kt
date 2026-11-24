package zel.wired.condition

import zel.wired.box.BoxSettings
import zel.wired.box.ConditionBox
import zel.wired.firing.Firing
import zel.wired.firing.Targets

class WearingEffect(
    override val furniId: Int,
    override val settings: BoxSettings,
    private val facts: UnitFacts,
    private val negative: Boolean,
) : ConditionBox {
    override fun passes(firing: Firing): Boolean {
        val users = Targets.units(firing, this)
        // Read as "any": an empty set fails the box and passes its not box.
        if (users.isEmpty()) return Quantified.noUsersPass(this) && Quantified.emptyHolds(false, negative)
        val effect = settings.intAt(0).coerceAtLeast(0)
        return users.any { facts.effect(it) == effect } != negative
    }
}
