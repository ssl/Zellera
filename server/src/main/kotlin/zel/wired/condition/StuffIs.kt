package zel.wired.condition

import zel.wired.box.BoxSettings
import zel.wired.box.ConditionBox
import zel.wired.firing.Firing
import zel.wired.firing.Targets

class StuffIs(
    override val furniId: Int,
    override val settings: BoxSettings,
    private val negative: Boolean,
) : ConditionBox {
    override fun passes(firing: Firing): Boolean {
        val types = Targets.furni(firing, this, COMPARE).map { it.definition.id }.toSet()
        val checked = Targets.furni(firing, this, CHECKED)
        if (checked.isEmpty() && Quantified.noFurniPass(this, CHECKED)) return Quantified.emptyHolds(settings, negative)
        val positive = Quantified.holds(checked, settings) { it.definition.id in types }
        return positive != negative
    }

    private companion object {
        const val CHECKED = 0
        const val COMPARE = 1
    }
}
