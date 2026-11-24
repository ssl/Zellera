package zel.protocol.incoming.users

import zel.game.players.Player
import zel.net.clients.ClientConnection
import zel.protocol.outgoing.nux.NewUserGiftMessage
import zel.protocol.outgoing.nux.TourHelpBubble
import zel.server.GameServices

class NewUserTour(
    private val services: GameServices,
) {
    fun proceed(player: Player) {
        val standing = player.stats.standing
        val step = standing.tourStep++
        val control = STEP_CONTROLS[step]
        if (control != null) {
            player.client?.send(TourHelpBubble(control, services.texts.text("tour.step.$step")))
            return
        }
        if (standing.welcomeGiftTaken || !giftsEnabled()) return
        val gifts = welcomeGifts()
        if (gifts.isNotEmpty()) player.client?.send(NewUserGiftMessage(listOf(gifts)))
    }

    fun pickGift(
        client: ClientConnection,
        index: Int,
    ) {
        val player = client.player ?: return
        val standing = player.stats.standing
        if (standing.welcomeGiftTaken || !giftsEnabled()) return
        val gift = welcomeGifts().getOrNull(index) ?: return
        standing.welcomeGiftTaken = true
        gift.give(player, checkNotNull(services.game.items))
    }

    private fun giftsEnabled(): Boolean = services.config.getBoolean("new_player.welcome_gift.is_offered", false)

    private fun welcomeGifts() = checkNotNull(services.game.items).sideTables.welcomeGifts()

    private companion object {
        val STEP_CONTROLS =
            mapOf(
                1 to "BOTTOM_BAR_RECEPTION",
                2 to "BOTTOM_BAR_NAVIGATOR",
                3 to "CHAT_INPUT",
                4 to "CHAT_HISTORY_BUTTON",
                5 to "MEMENU_CLOTHES",
                6 to "BOTTOM_BAR_CATALOGUE",
                7 to "CREDITS_BUTTON",
                8 to "POINTS_BUTTON",
                9 to "PREMIUM_BUTTON",
                10 to "FRIENDS_BAR_ALL_FRIENDS",
                11 to "BOTTOM_BAR_NAVIGATOR",
            )
    }
}
