package zel.game.achievements

import zel.game.players.Player

/** Counts [amount] of a game event (see [GameEvents]) for a player. */
fun interface AchievementTally {
    fun add(
        player: Player?,
        event: String,
        amount: Int,
    )

    companion object {
        fun running(): AchievementTally = AchievementEvents.tally()
    }
}
