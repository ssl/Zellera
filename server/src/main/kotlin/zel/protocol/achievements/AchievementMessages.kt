package zel.protocol.achievements

import zel.net.clients.ClientConnection
import zel.protocol.generated.AchievementNotificationOut
import zel.protocol.generated.AchievementNotificationOutCodec
import zel.protocol.generated.AchievementOut
import zel.protocol.generated.AchievementOutCodec
import zel.protocol.generated.AchievementsOut
import zel.protocol.generated.AchievementsOutCodec

class AchievementMessages {
    fun list(
        client: ClientConnection,
        entries: List<AchievementEntry>,
        defaultCategory: String = "",
    ) = client.send(AchievementsOutCodec, listMessage(entries, defaultCategory))

    fun progress(
        client: ClientConnection,
        entry: AchievementEntry,
    ) = client.send(AchievementOutCodec, progressMessage(entry))

    fun levelUp(
        client: ClientConnection,
        levelUp: AchievementLevelUp,
    ) = client.send(AchievementNotificationOutCodec, levelUpMessage(levelUp))

    companion object {
        fun listMessage(
            entries: List<AchievementEntry>,
            defaultCategory: String = "",
        ) = AchievementsOut(
            entries.map {
                AchievementsOut.List1Item(
                    it.id,
                    it.step.level,
                    it.step.badge,
                    it.step.from,
                    it.step.to,
                    it.reward.amount,
                    it.reward.currency,
                    it.progress,
                    it.finished,
                    it.listing.category,
                    it.listing.subcategory,
                    it.listing.steps,
                    it.listing.method,
                    it.state,
                )
            },
            defaultCategory,
        )

        fun progressMessage(e: AchievementEntry) =
            AchievementOut(
                e.id,
                e.step.level,
                e.step.badge,
                e.step.from,
                e.step.to,
                e.reward.amount,
                e.reward.currency,
                e.progress,
                e.finished,
                e.listing.category,
                e.listing.subcategory,
                e.listing.steps,
                e.listing.method,
                e.state,
            )

        fun levelUpMessage(l: AchievementLevelUp) =
            AchievementNotificationOut(
                l.achievementId,
                l.level,
                l.badgeId,
                l.badgeCode,
                l.points,
                l.rewardAmount,
                l.rewardType,
                l.bonusPoints,
                l.levelId,
                l.previousBadgeCode,
                l.category,
                l.showDialog,
                l.badgeOwnerCount,
                l.badgeRarityTier,
            )
    }
}
