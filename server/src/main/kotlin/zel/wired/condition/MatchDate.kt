package zel.wired.condition

import zel.wired.box.BoxSettings
import zel.wired.box.ConditionBox
import zel.wired.firing.Firing

class MatchDate(
    override val furniId: Int,
    override val settings: BoxSettings,
    private val clock: WiredClock,
) : ConditionBox {
    override fun passes(firing: Firing): Boolean {
        val date = clock.now(firing.scope.room).toLocalDate()
        return allowed(settings.intAt(WEEKDAYS, ALL_WEEKDAYS) and ALL_WEEKDAYS, date.dayOfWeek.value - 1) &&
            allowed(settings.intAt(MONTHS, ALL_MONTHS) and ALL_MONTHS, date.monthValue - 1) &&
            (
                settings.intAt(DAY_USED) == 0 ||
                    ClockRange.contains(date.dayOfMonth, settings.intAt(DAY_MIN), settings.intAt(DAY_MAX))
            ) &&
            (
                settings.intAt(YEAR_USED) == 0 ||
                    ClockRange.contains(date.year, settings.intAt(YEAR_MIN), settings.intAt(YEAR_MAX))
            )
    }

    /** A mask of 0 ticks nothing and fails. */
    private fun allowed(
        mask: Int,
        bit: Int,
    ): Boolean = (mask shr bit) and 1 != 0

    private companion object {
        const val DAY_USED = 0
        const val YEAR_USED = 1
        const val WEEKDAYS = 2
        const val DAY_MIN = 3
        const val DAY_MAX = 4
        const val MONTHS = 5
        const val YEAR_MIN = 6
        const val YEAR_MAX = 7
        const val ALL_WEEKDAYS = 127
        const val ALL_MONTHS = 4095
    }
}
