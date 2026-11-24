package zel.game.achievements

/** Badge codes of achievement levels: `ACH_<name><level>`, bare `ACH_<name>` for a one-level name ending in a digit. */
object AchievementBadges {
    private const val PREFIX = "ACH_"

    fun code(
        achievement: String,
        levelCount: Int,
        level: Int,
    ): String = if (bare(achievement, levelCount)) "$PREFIX$achievement" else "$PREFIX$achievement$level"

    private fun bare(
        achievement: String,
        levelCount: Int,
    ) = levelCount == 1 && achievement.lastOrNull()?.isDigit() == true
}
