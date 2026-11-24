package zel.game.players.subscriptions

import zel.game.achievements.AchievementEvents
import zel.game.catalog.BuildPassMembership
import zel.game.players.Player
import zel.protocol.outgoing.generic.alerts.BubbleAlert
import zel.protocol.outgoing.generic.alerts.BubbleAlertMessage
import zel.server.GameServices

class SubscriptionBuildPass(
    private val services: GameServices,
) : SubscriptionReactions {
    override fun onStarted(subscription: Subscription) = tell(subscription, BubbleAlert.BUILD_PASS_JOINED)

    override fun onExtended(
        subscription: Subscription,
        bySeconds: Int,
    ) = tell(subscription, BubbleAlert.BUILD_PASS_EXTENDED, extended = true)

    override fun onExpired(subscription: Subscription) = tell(subscription, notice = null)

    private fun tell(
        subscription: Subscription,
        notice: BubbleAlert?,
        extended: Boolean = false,
    ) {
        val member: Player = services.onlinePlayer(subscription.playerId) ?: return
        val unlocked = member.rooms.syncBuildPassRooms(member.profile.id)
        member.client?.let { client ->
            if (notice != null &&
                unlocked > 0
            ) {
                client.send(BubbleAlertMessage(BubbleAlert.BUILD_PASS_ROOM_OPENED.key))
            }
            BuildPassMembership.tellStatus(member, services.database)
            if (notice != null) {
                val limit = BuildPassMembership.furniLimit(member.profile).toString()
                client.send(BubbleAlertMessage(notice.key, mapOf("furni_limit" to limit)))
            }
        }
        AchievementEvents.identity().mirrorBuildPass(member, extended)
    }
}
