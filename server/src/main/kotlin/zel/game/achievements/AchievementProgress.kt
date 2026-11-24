package zel.game.achievements

import zel.game.players.Player

interface AchievementProgress {
    /** The achievements the content pack counts on [event]: "name", or "name:detail" (see [GameEvents]). */
    fun forEvent(event: String): List<Achievement>

    fun progress(
        player: Player?,
        achievement: Achievement?,
        amount: Int,
    )

    fun progressFor(
        playerId: Int,
        achievement: Achievement?,
        amount: Int,
    )

    fun announce(
        player: Player,
        achievement: Achievement,
    )

    companion object {
        fun queueKey(playerId: Int): Any = QueueKey(playerId)

        private data class QueueKey(
            val playerId: Int,
        )

        val NONE: AchievementProgress =
            object : AchievementProgress {
                override fun forEvent(event: String): List<Achievement> = emptyList()

                override fun progress(
                    player: Player?,
                    achievement: Achievement?,
                    amount: Int,
                ) = Unit

                override fun progressFor(
                    playerId: Int,
                    achievement: Achievement?,
                    amount: Int,
                ) = Unit

                override fun announce(
                    player: Player,
                    achievement: Achievement,
                ) = Unit
            }
    }
}

/** Counts [amount] of [event] for an online [player] on every achievement the content pack hangs on it. */
fun AchievementProgress.count(
    player: Player?,
    event: String,
    amount: Int = 1,
) = forEvent(event).forEach { progress(player, it, amount) }

/** As [count], for a player who may be away. */
fun AchievementProgress.countFor(
    playerId: Int,
    event: String,
    amount: Int = 1,
) = forEvent(event).forEach { progressFor(playerId, it, amount) }
