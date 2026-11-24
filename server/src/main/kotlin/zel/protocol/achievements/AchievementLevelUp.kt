package zel.protocol.achievements

/** Fields in wire order. */
data class AchievementLevelUp(
    val achievementId: Int,
    val level: Int,
    val badgeId: Int,
    val badgeCode: String,
    val points: Int,
    val rewardAmount: Int,
    val rewardType: Int,
    val bonusPoints: Int,
    val levelId: Int,
    val previousBadgeCode: String,
    val category: String,
    val showDialog: Boolean,
    val badgeOwnerCount: Int,
    val badgeRarityTier: Int,
)
