package zel.game.moderation

import zel.util.SECONDS_PER_DAY
import zel.util.SECONDS_PER_HOUR

internal object SanctionLadderMath {
    /** When a probation starting at [start] ends; capped, the last step has 876581 days. */
    fun probationEnd(
        start: Int,
        probationDays: Int,
    ): Int = capped(start.toLong() + probationDays.toLong() * SECONDS_PER_DAY)

    /** How long a ban of [hours] lasts in seconds; capped against [now], the last step is 100 years. */
    fun banSeconds(
        hours: Int,
        now: Int,
    ): Int = minOf(hours.toLong() * SECONDS_PER_HOUR, Int.MAX_VALUE.toLong() - now).toInt()

    private fun capped(seconds: Long): Int = minOf(seconds, Int.MAX_VALUE.toLong()).toInt()
}
