package zel.game.achievements

/** A row of `achievement_triggers`: [achievementId] counts on [event], only for [detail] when it is not empty. */
data class AchievementTrigger(
    val achievementId: Int,
    val event: String,
    val detail: String,
)
