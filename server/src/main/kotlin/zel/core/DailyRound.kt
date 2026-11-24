package zel.core

import zel.util.SECONDS_PER_HOUR

/**
 * The daily respects, the daily pet scratches and the daily tasks start over together every [hours] hours. Rounds
 * are counted from 1970, so every part of the game and every restart agree on where one ends.
 */
class DailyRound(
    private val hours: () -> Int = { STANDARD_HOURS },
) {
    val seconds: Long get() = hours().coerceIn(1, MAX_HOURS) * SECONDS_PER_HOUR.toLong()

    fun numberAt(epochSecond: Long): Long = Math.floorDiv(epochSecond, seconds)

    fun endOf(number: Long): Long = (number + 1) * seconds

    companion object {
        const val SETTING = "game.daily_reset.interval.hours"
        const val STANDARD_HOURS = 18
        private const val MAX_HOURS = 24 * 7

        fun of(config: Configuration) = DailyRound { config.getInt(SETTING, STANDARD_HOURS) }
    }
}
