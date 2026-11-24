package zel.game.games.snowball

import zel.game.achievements.Achievement

/** Lobby skill fields from the total-score achievement: level max(1, its level), progress, next threshold. */
data class SnowballSkill(
    val skillLevel: Int,
    val totalScore: Int,
    val scoreToNextLevel: Int,
) {
    companion object {
        val NONE = SnowballSkill(1, 0, 0)

        fun of(
            achievement: Achievement?,
            progress: Int,
        ): SnowballSkill {
            if (achievement == null) {
                return SnowballSkill(1, maxOf(0, progress), 0)
            }
            val thresholds =
                achievement.details.ordered
                    .map { it.progressNeeded }
                    .toIntArray()
            return fromThresholds(thresholds, progress)
        }

        internal fun fromThresholds(
            thresholds: IntArray,
            progress: Int,
        ): SnowballSkill {
            val score = maxOf(0, progress)
            val level = thresholds.indices.firstOrNull { score < thresholds[it] } ?: thresholds.size
            val next = if (thresholds.isEmpty()) 0 else thresholds[minOf(level, thresholds.size - 1)]
            return SnowballSkill(maxOf(1, level), score, next)
        }
    }
}
