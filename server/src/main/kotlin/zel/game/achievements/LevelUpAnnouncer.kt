package zel.game.achievements

import zel.game.earnings.IncomeRewards
import zel.game.economy.EconomyLedger
import zel.game.economy.LedgerEntry
import zel.game.players.Player
import zel.game.players.PlayerBadge
import zel.game.players.inventory.BadgeCaches
import zel.game.players.level.AccountLevels
import zel.protocol.Outbound
import zel.protocol.achievements.AchievementLevelUp
import zel.protocol.achievements.AchievementMessages
import zel.protocol.generated.ActivityPointNotificationOut
import zel.protocol.generated.ActivityPointNotificationOutCodec
import zel.protocol.generated.IncomeRewardNotificationOut
import zel.protocol.generated.IncomeRewardNotificationOutCodec
import zel.protocol.outgoing.inventory.AddRoomItem
import zel.protocol.outgoing.inventory.AddRoomItem.UnseenCategory
import zel.protocol.outgoing.users.UserAchievementScore
import zel.protocol.outgoing.users.UserBadges
import zel.protocol.outgoing.users.UserCredits

internal class LevelUpAnnouncer(
    private val messages: AchievementMessages,
    private val accountLevels: AccountLevels?,
    private val badgeCaches: BadgeCaches,
) {
    sealed interface Message {
        data class Composed(
            val outbound: Outbound<*>?,
        ) : Message

        data class Unlocked(
            val levelUp: AchievementLevelUp,
        ) : Message
    }

    fun announce(
        player: Player,
        achievement: Achievement,
        earned: List<AchievementLevel>,
        badge: PlayerBadge?,
        rewards: LevelRewards,
    ) {
        val messages = messagesFor(player, achievement, earned, badge, rewards.income.isNotEmpty(), rewards.wallet)
        // A new badge changes the score the last message shows, so the account level is counted again first.
        if (badge != null) accountLevels?.recountLogged(player.profile.id)
        messages.forEach { send(player, it) }
        if (badge != null) updateRoom(player, badge)
    }

    fun messagesFor(
        player: Player,
        achievement: Achievement,
        earned: List<AchievementLevel>,
        badge: PlayerBadge?,
        earnings: Boolean,
        walletRewards: List<LedgerEntry>,
    ): List<Message> =
        buildList {
            if (badge != null) add(Message.Composed(AddRoomItem(badge.id, UnseenCategory.BADGE).compose()))
            if (earnings) {
                add(
                    Message.Composed(
                        Outbound(
                            IncomeRewardNotificationOutCodec,
                            IncomeRewardNotificationOut(IncomeRewards.ACHIEVEMENTS.toByte()),
                        ),
                    ),
                )
                add(Message.Composed(IncomeRewards.unseenEarning().compose()))
            }
            walletRewards.mapTo(this) { Message.Composed(purse(player, it)) }
            if (badge != null) {
                earned.mapTo(this) { level ->
                    Message.Unlocked(AchievementEntries.levelUp(achievement, level, badge.id, badgeCaches.ownerCounts))
                }
                add(Message.Composed(UserAchievementScore(player).compose()))
            }
        }

    private fun purse(
        player: Player,
        reward: LedgerEntry,
    ): Outbound<*>? =
        when (reward.currency) {
            EconomyLedger.CREDITS -> {
                UserCredits(player).compose()
            }

            else -> {
                Outbound(
                    ActivityPointNotificationOutCodec,
                    ActivityPointNotificationOut(
                        player.profile.balance(reward.currency),
                        reward.amount,
                        reward.currency,
                    ),
                )
            }
        }

    private fun send(
        player: Player,
        message: Message,
    ) = when (message) {
        is Message.Composed -> player.client?.send(message.outbound)
        is Message.Unlocked -> player.client?.let { messages.levelUp(it, message.levelUp) }
    }

    private fun updateRoom(
        player: Player,
        badge: PlayerBadge,
    ) {
        val playerId = player.profile.id
        val rooms = player.services.game.rooms
        val roomId = rooms.roomOf(playerId) ?: return
        if (badge.slot > 0) {
            val wearing = player.inventory.badges.worn
            UserBadges(wearing, playerId, badgeCaches.ownerCounts).compose()?.let { rooms.send(roomId, it) }
        }
        rooms.appearanceChanged(playerId)
    }
}
