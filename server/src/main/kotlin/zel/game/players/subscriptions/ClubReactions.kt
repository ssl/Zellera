package zel.game.players.subscriptions

import zel.core.rules.rules
import zel.game.achievements.GameEvents
import zel.game.players.Player
import zel.game.players.PlayerProfile
import zel.game.players.figure.LookValidation
import zel.game.players.required
import zel.protocol.generated.ClubGiftNotificationOut
import zel.protocol.generated.ClubGiftNotificationOutCodec
import zel.protocol.outgoing.users.UpdateUserLook
import zel.protocol.outgoing.users.UserClub
import zel.protocol.outgoing.users.UserPermissions
import zel.server.GameServices
import zel.util.GameZone

class ClubReactions(
    private val services: GameServices,
) : SubscriptionReactions {
    private val players get() = services.game.players.required()

    override fun onStarted(subscription: Subscription) {
        val profile = players.profileOf(subscription.playerId) ?: return
        val stats = profile.loadStats()
        val payday = ClubSettings.payday
        val now = services.unixTimestamp()
        stats.standing.maxFriends = rules.maxFriendsClub
        stats.standing.maxRooms = rules.maxRoomsClub
        val periodStart = PaydaySchedule.latestAtOrBefore(now, payday.dayOfMonth, GameZone.current)
        stats.club.lastPayday = if (payday.spendingFromStart) now else periodStart
        stats.clubLastChangedAt = now
        services.threading.run(stats)
        players.find(subscription.playerId)?.let(::tellStatus)
        syncAchievement(services, profile)
    }

    override fun onExtended(
        subscription: Subscription,
        bySeconds: Int,
    ) {
        val player = players.find(subscription.playerId) ?: return
        player.stats.clubLastChangedAt = services.unixTimestamp()
        tellStatus(player)
        syncAchievement(services, player.profile)
    }

    override fun onExpired(subscription: Subscription) {
        val profile = players.profileOf(subscription.playerId) ?: return
        val player = players.find(subscription.playerId)
        val stats = profile.loadStats()
        stats.standing.maxFriends = rules.maxFriends
        stats.standing.maxRooms = rules.maxRooms
        services.threading.run(stats)
        if (player != null && LookValidation.validateOnHcExpire) {
            profile.figure = LookValidation.allowedLook(player, profile.figure, profile.gender.name)
            services.threading.run(profile)
            player.client?.send(UpdateUserLook(player))
            services.game.rooms.appearanceChanged(profile.id)
        }
        player?.let(::tellStatus)
    }

    private fun tellStatus(player: Player) {
        val client = player.client ?: return
        client.send(UserPermissions(player))
        client.send(UserClub(player, UserClub.RESPONSE_PUSHED))
        giftNotice(player)
    }

    companion object {
        fun loggedIn(player: Player) {
            syncAchievement(player.services, player.profile)
            giftNotice(player)
        }

        private fun giftNotice(player: Player) {
            val gifts = player.stats.club.remainingGifts
            if (rules.monthlyGiftNotice &&
                gifts > 0
            ) {
                player.client?.send(ClubGiftNotificationOutCodec, ClubGiftNotificationOut(gifts))
            }
        }

        private fun syncAchievement(
            services: GameServices,
            profile: PlayerProfile,
        ) {
            val achievements =
                services.game.achievements
                    ?.forEvent(GameEvents.CLUB_DAYS)
                    .orEmpty()
            if (achievements.isEmpty()) return
            val stats = profile.loadStats()
            for (achievement in achievements) {
                val missing = stats.club.totalClubDays - maxOf(stats.progressOf(achievement), 0)
                if (missing > 0) achievement.progressFor(profile.id, missing)
            }
        }
    }
}
