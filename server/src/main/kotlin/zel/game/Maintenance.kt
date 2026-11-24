package zel.game

import zel.core.Configuration

/** Keeps ranks below `maintenance.lowest_rank_in` out during maintenance; the same ranks stay in past closing time. */
class Maintenance(
    private val settings: Configuration,
) {
    private val active: Boolean get() = settings.getBoolean(ON_SETTING, false)

    private val lowestRankIn: Int get() = settings.getInt(LOWEST_RANK_SETTING, STANDARD_LOWEST_RANK)

    val durationMinutes: Int get() = settings.getInt(DURATION_SETTING, STANDARD_DURATION_MINUTES)

    fun admits(rank: Int): Boolean = !active || letsThrough(rank)

    fun letsThrough(rank: Int): Boolean = rank >= lowestRankIn

    companion object {
        const val ON_SETTING = "maintenance.is_active"
        const val LOWEST_RANK_SETTING = "maintenance.lowest_rank_in"
        const val DURATION_SETTING = "maintenance.duration.minutes"
        const val STANDARD_LOWEST_RANK = 5
        const val STANDARD_DURATION_MINUTES = 15
    }
}
