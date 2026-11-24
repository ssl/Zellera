package zel.game.achievements

import zel.game.players.Player
import zel.game.players.inventory.BadgeHolders
import zel.game.players.inventory.BadgeTier
import zel.protocol.achievements.AchievementEntry
import zel.protocol.achievements.AchievementLevelUp

object AchievementEntries {
    fun listFor(
        player: Player,
        achievements: Achievements,
    ): List<AchievementEntry> =
        achievements.all.values
            .filter { it.levels.isNotEmpty() }
            .sortedBy { it.id }
            .mapNotNull { entryOrNull(player, it) }

    fun entry(
        player: Player,
        achievement: Achievement,
    ): AchievementEntry =
        entryOrNull(player, achievement)
            ?: AchievementEntry(
                id = achievement.id,
                step = AchievementEntry.Step(0, "", 0, 0),
                reward = AchievementEntry.Reward(0, 0),
                progress = 0,
                finished = false,
                listing = listing(achievement, steps = 0),
                state = achievement.state,
            )

    private fun entryOrNull(
        player: Player,
        achievement: Achievement,
    ): AchievementEntry? {
        val progress = maxOf(0, player.stats.progressOf(achievement))
        val reached = achievement.levelAt(progress)?.level ?: 0
        val target = achievement.levelAfter(reached) ?: achievement.lastLevel ?: return null
        return AchievementEntry(
            id = achievement.id,
            step =
                AchievementEntry.Step(
                    level = target.level,
                    badge = achievement.badgeCode(target.level),
                    from = achievement.levelBefore(target.level)?.progressNeeded ?: 0,
                    to = target.progressNeeded,
                ),
            reward = AchievementEntry.Reward(target.rewardAmount, target.rewardCurrency),
            progress = progress,
            finished = achievement.isFinishedAt(progress),
            listing = listing(achievement, achievement.levels.size),
            state = achievement.state,
        )
    }

    private fun listing(
        achievement: Achievement,
        steps: Int,
    ) = AchievementEntry.Listing(
        achievement.category.shownAs,
        achievement.subcategory,
        steps,
        achievement.displayMethod,
    )

    fun levelUp(
        achievement: Achievement,
        level: AchievementLevel,
        badgeId: Int,
        ownerCounts: BadgeHolders,
    ): AchievementLevelUp {
        val badgeCode = achievement.badgeCode(level.level)
        val owners = ownerCounts.count(badgeCode)
        return AchievementLevelUp(
            achievementId = achievement.id,
            level = level.level,
            badgeId = badgeId,
            badgeCode = badgeCode,
            points = level.scorePoints,
            rewardAmount = level.rewardAmount,
            rewardType = level.rewardCurrency,
            bonusPoints = level.bonusPoints,
            levelId = level.notificationId.takeIf { it > 0 } ?: achievement.id,
            previousBadgeCode = achievement.levelBefore(level.level)?.let { achievement.badgeCode(it.level) }.orEmpty(),
            category = achievement.category.shownAs,
            showDialog = achievement.countsOn(GameEvents.CLUB_DAYS),
            badgeOwnerCount = owners,
            badgeRarityTier = BadgeTier.forHolders(owners).code,
        )
    }
}
