package zel.wired.condition

import zel.wired.box.BoxSettings
import zel.wired.box.ConditionBox
import zel.wired.firing.Firing
import zel.wired.firing.Targets

class ChestHasItemType(
    override val furniId: Int,
    override val settings: BoxSettings,
    private val chests: ChestStock,
    private val amounts: ChestAmounts,
) : ConditionBox {
    override fun passes(firing: Firing): Boolean {
        val types = Targets.furni(firing, this, TYPES).map { it.definition.id }.toSet()
        val boxes = Targets.furni(firing, this, CHESTS)
        if (boxes.isEmpty() && Quantified.noFurniPass(this, CHESTS)) return true
        if (types.isEmpty()) return false
        val count = boxes.mapNotNull { chests.count(firing.scope, it, types) }.sum()
        val amount = amounts.amount(firing, this) ?: return false
        return Comparison.holds(Comparison.anyOfSix(settings.intAt(COMPARISON)), count, amount)
    }

    private companion object {
        const val TYPES = 0
        const val CHESTS = 1
        const val COMPARISON = 3
    }
}
