package zel.game.achievements

internal object AchievementProgression {
    /** Achievement states that never count: 0, archived (2). */
    private val NOT_COUNTED_STATES = setOf<Short>(0, 2)

    sealed interface Count {
        data object Nothing : Count

        /** The count ends at [to]; [reachesLevel] when a level lies between the two. */
        data class Added(
            val to: Int,
            val reachesLevel: Boolean,
        ) : Count
    }

    fun counts(achievement: Achievement): Boolean = achievement.state !in NOT_COUNTED_STATES

    /** [known] is what the session knows of the stored progress, -1 for a player who never had any. */
    fun count(
        achievement: Achievement,
        known: Int,
        amount: Int,
    ): Count {
        val from = maxOf(0, known)
        val to = minOf(Int.MAX_VALUE.toLong(), from.toLong() + amount).toInt()
        return when {
            !counts(achievement) || achievement.isFinishedAt(from) || to == from -> Count.Nothing
            else -> Count.Added(to, levelNumberAt(achievement, to) > levelNumberAt(achievement, from))
        }
    }

    /** The levels [progress] reaches that were not handed out yet, lowest first. */
    fun owed(
        achievement: Achievement,
        progress: Int,
        rewardedLevel: Int,
    ): List<AchievementLevel> {
        if (!counts(achievement)) return emptyList()
        val reached = levelNumberAt(achievement, progress)
        return achievement.details.ordered.filter { it.level > rewardedLevel && it.level <= reached }
    }

    private fun levelNumberAt(
        achievement: Achievement,
        progress: Int,
    ): Int = achievement.levelAt(progress)?.level ?: 0
}
