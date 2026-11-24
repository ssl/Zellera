package zel.game.games.snowball

import zel.core.rules.GameRules

data class SnowballAllowance(
    val playedToday: Int,
    val tokens: Int,
    val club: Boolean,
) {
    fun dailyLimit(rules: GameRules): Int = if (club) rules.snowballDailyFreeGamesClub else rules.snowballDailyFreeGames

    fun gamesLeft(rules: GameRules): Int = maxOf(0, dailyLimit(rules) - playedToday) + tokens

    fun nextIsFree(rules: GameRules): Boolean = playedToday < dailyLimit(rules)
}
