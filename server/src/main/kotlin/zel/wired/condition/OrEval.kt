package zel.wired.condition

import zel.wired.box.BoxSettings
import zel.wired.box.ConditionMode

class OrEval(
    override val furniId: Int,
    override val settings: BoxSettings,
) : ConditionMode {
    /** The picked boxes, whatever the furni source says. */
    override val appliesTo: List<Int> = settings.picks.distinct()

    override fun passes(results: List<Boolean>): Boolean {
        val passed = results.count { it }
        return when (settings.intAt(0, AT_LEAST_ONE)) {
            ALL -> passed == results.size
            AT_LEAST_ONE -> passed >= 1
            NOT_ALL -> passed < results.size
            NONE -> passed == 0
            COUNT -> counted(passed)
            else -> passed == results.size
        }
    }

    private fun counted(passed: Int): Boolean {
        val number = settings.intAt(2)
        return when (settings.intAt(1)) {
            Comparison.LESS -> passed < number
            Comparison.EQUAL -> passed == number
            Comparison.MORE -> passed > number
            else -> false
        }
    }

    companion object {
        const val ALL = 0
        const val AT_LEAST_ONE = 1
        const val NOT_ALL = 2
        const val NONE = 3
        const val COUNT = -1
    }
}
