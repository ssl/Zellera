package zel.wired.condition

import zel.wired.box.BoxSettings
import zel.wired.box.ConditionBox
import zel.wired.firing.Firing
import zel.wired.firing.Targets

class SelectionQuantity(
    override val furniId: Int,
    override val settings: BoxSettings,
) : ConditionBox {
    override fun passes(firing: Firing): Boolean {
        val count =
            if (settings.intAt(0) ==
                USERS
            ) {
                Targets.units(firing, this).size
            } else {
                Targets.furni(firing, this).size
            }
        return Comparison.holds(settings.intAt(COMPARISON), count, settings.intAt(AMOUNT).coerceIn(0, MAX_AMOUNT))
    }

    companion object {
        private const val USERS = 1
        private const val COMPARISON = 2
        const val AMOUNT = 1
        const val MAX_AMOUNT = 100
    }
}
