package zel.game

import zel.core.Configuration

class OpeningHoursAccess(
    private val settings: Configuration,
) {
    private val lowestRankIn: Int get() = settings.getInt(LOWEST_RANK_SETTING, STANDARD_LOWEST_RANK)

    fun letsThrough(rank: Int): Boolean = rank >= lowestRankIn

    companion object {
        const val LOWEST_RANK_SETTING = "maintenance.lowest_rank_in"
        const val STANDARD_LOWEST_RANK = 5
    }
}
