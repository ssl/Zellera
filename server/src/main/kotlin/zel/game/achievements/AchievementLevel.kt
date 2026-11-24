package zel.game.achievements

/** [notificationId] 0 means the level is announced under the achievement's id. */
data class AchievementLevel(
    val level: Int,
    val progressNeeded: Int,
    val scorePoints: Int = 0,
    val rewardCurrency: Int = 0,
    val rewardAmount: Int = 0,
    val notificationId: Int = 0,
    val bonusPoints: Int = 0,
)
