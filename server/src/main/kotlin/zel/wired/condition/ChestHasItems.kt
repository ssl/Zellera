package zel.wired.condition

import zel.wired.box.BoxSettings
import zel.wired.box.ConditionBox
import zel.wired.firing.Firing
import zel.wired.firing.Targets

class ChestHasItems(
    override val furniId: Int,
    override val settings: BoxSettings,
    private val chests: ChestStock,
    private val amounts: ChestAmounts,
) : ConditionBox {
    /**
     * No chests from a set is nothing to check: the box passes, "all" and "any" alike. An absent triggering
     * furni passes an "all" box; an "any" box compares 0 against the amount.
     */
    override fun passes(firing: Firing): Boolean {
        val targets = Targets.furni(firing, this)
        if (targets.isEmpty() && (Quantified.noFurniPass(this) || settings.quantifier != ANY)) return true
        val total = targets.mapNotNull { chests.total(firing.scope, it) }.sum()
        val amount = amounts.amount(firing, this) ?: return false
        return Comparison.holds(Comparison.anyOfSix(settings.intAt(COMPARISON)), total, amount)
    }

    private companion object {
        const val COMPARISON = 3
        const val ANY = 1
    }
}
