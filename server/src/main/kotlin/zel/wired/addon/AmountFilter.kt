package zel.wired.addon

import zel.wired.box.BoxSettings
import zel.wired.box.SelectionFilter
import zel.wired.firing.Firing
import kotlin.random.Random

abstract class AmountFilter(
    final override val furniId: Int,
    final override val settings: BoxSettings,
    private val amounts: FilterAmounts,
    private val random: Random,
) : SelectionFilter {
    override val rank = RANK

    private var cutFor: Firing? = null
    private var seed = 0L

    protected fun <T> cut(
        firing: Firing,
        items: List<T>,
    ): List<T> {
        val amount = amount(firing) ?: return items
        if (items.size <= amount) return items
        if (cutFor !== firing) {
            cutFor = firing
            seed = random.nextLong()
        }
        val kept =
            items.indices
                .shuffled(Random(seed))
                .take(amount)
                .toSet()
        return items.filterIndexed { index, _ -> index in kept }
    }

    private fun amount(firing: Firing): Int? =
        if (settings.intAt(FROM_VARIABLE) != 0) {
            amounts.fromVariable(firing, this).coerceAtLeast(0)
        } else {
            settings.intAt(AMOUNT, FRESH_AMOUNT).takeIf { it > 0 }
        }

    companion object {
        /** Plain filters run after the by-variable ones. */
        const val RANK = 1
        private const val AMOUNT = 0
        private const val FROM_VARIABLE = 1
        private const val FRESH_AMOUNT = 10
    }
}
