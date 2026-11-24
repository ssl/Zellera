package zel.protocol.handshake

import zel.util.SECONDS_PER_DAY

/** Login days are UTC calendar days of Unix second stamps; a stamp of 0 or less means never. */
object LoginCalendar {
    private fun dayOf(stamp: Int): Int = Math.floorDiv(stamp, SECONDS_PER_DAY)

    fun daysBetween(
        earlier: Int,
        later: Int,
    ): Int = dayOf(later) - dayOf(earlier)

    fun isFirstOfDay(
        previousLogin: Int,
        now: Int,
    ): Boolean = previousLogin <= 0 || dayOf(previousLogin) < dayOf(now)

    fun streak(
        current: Int,
        previousLogin: Int,
        now: Int,
    ): Int {
        if (previousLogin <= 0) return 1
        return when (daysBetween(previousLogin, now)) {
            0 -> maxOf(current, 1)
            1 -> current + 1
            else -> 1
        }
    }
}
