package zel.protocol.handshake

import zel.core.Configuration
import zel.core.rules.rules
import zel.game.players.Player
import zel.game.players.figure.LookValidation
import zel.game.players.inventory.UnseenItems
import zel.game.rewardtracks.RewardTrackService
import zel.net.clients.ClientConnection
import zel.protocol.generated.AuthenticationOKOut
import zel.protocol.generated.AuthenticationOKOutCodec
import zel.protocol.generated.AvailabilityStatusOut
import zel.protocol.generated.AvailabilityStatusOutCodec
import zel.protocol.generated.InfoFeedEnableOut
import zel.protocol.generated.InfoFeedEnableOutCodec
import zel.protocol.generated.IsFirstLoginOfDayOut
import zel.protocol.generated.IsFirstLoginOfDayOutCodec
import zel.protocol.generated.NavigatorSettingsOut
import zel.protocol.generated.NavigatorSettingsOutCodec
import zel.protocol.generated.NewPlayerExperienceNotCompleteOut
import zel.protocol.generated.NewPlayerExperienceNotCompleteOutCodec
import zel.protocol.generated.NoobnessLevelOut
import zel.protocol.generated.NoobnessLevelOutCodec
import zel.protocol.generated.OutgoingCodec
import zel.protocol.generated.PlayerEffectSelectedOut
import zel.protocol.generated.PlayerEffectSelectedOutCodec
import zel.protocol.outgoing.OutgoingMessage
import zel.protocol.outgoing.catalog.CatalogReplies
import zel.protocol.outgoing.icons.IconShopData
import zel.protocol.outgoing.icons.PlayerIcons
import zel.protocol.outgoing.inventory.AddRoomItem
import zel.protocol.outgoing.inventory.PlayerEffects
import zel.protocol.outgoing.mysterybox.MysteryBoxKeys
import zel.protocol.outgoing.users.Favourites
import zel.protocol.outgoing.users.FigureSetIds
import zel.protocol.outgoing.users.UserAchievementScore
import zel.protocol.outgoing.users.UserClub
import zel.protocol.outgoing.users.UserCurrency
import zel.protocol.outgoing.users.UserPermissions
import java.util.TreeMap

/** Login frames whose order is fixed; the unordered rest is in [LoginFollowUps]. */
class LoginBurst(
    private val config: Configuration,
    private val rewardTracks: RewardTrackService?,
    private val now: () -> Int,
    private val roomExists: (Int) -> Boolean,
) {
    /** The room the client enters on its own at login: the home room, else the default room; 0 for none. */
    private fun roomToEnter(homeRoomId: Int): Int =
        when {
            !rules.enterHomeRoom -> 0
            homeRoomId > 0 -> homeRoomId
            rules.loginDefaultRoom > 0 && roomExists(rules.loginDefaultRoom) -> rules.loginDefaultRoom
            else -> 0
        }

    fun sendTo(
        client: ClientConnection,
        player: Player,
    ) {
        if (LookValidation.validateOnLogin) {
            val look = LookValidation.allowedLook(player)
            if (look != player.profile.figure) {
                player.profile.figure = look
                Thread.ofVirtual().start(player.profile)
            }
        }
        Sends(client).apply {
            val profile = player.profile
            val effects = player.inventory.effects
            typed(AuthenticationOKOutCodec, AuthenticationOKOut(profile.id, emptyList(), profile.id))
            composed(PlayerEffects(player, effects.owned))
            typed(NavigatorSettingsOutCodec, NavigatorSettingsOut(profile.homeRoomId, roomToEnter(profile.homeRoomId)))
            composed(Favourites(player))
            composed(UserClub(player, UserClub.RESPONSE_PUSHED))
            CatalogReplies.buildPassStatus(client, player)
            composed(unseenItems(player.inventory.unseen))
            composed(FigureSetIds(player))
            rewardTracks?.sendTracks(client, profile.id)
            val icons = player.iconService.rosterOf(player)
            composed(PlayerIcons(icons))
            composed(IconShopData(icons))
            if (icons.unseen.isNotEmpty()) {
                composed(AddRoomItem(icons.unseen.toIntArray(), AddRoomItem.UnseenCategory.ICON))
            }
            effects.worn.takeIf { it != 0 }?.let { typed(PlayerEffectSelectedOutCodec, PlayerEffectSelectedOut(it)) }
            typed(NoobnessLevelOutCodec, NoobnessLevelOut(NoobnessLevel.of(profile)))
            if (!player.stats.standing.welcomeGiftTaken && config.getBoolean("new_player.welcome_gift.is_offered")) {
                typed(NewPlayerExperienceNotCompleteOutCodec, NewPlayerExperienceNotCompleteOut)
            }
            composed(UserPermissions(player))
            typed(AvailabilityStatusOutCodec, AvailabilityStatusOut(true, false, true))
            typed(InfoFeedEnableOutCodec, InfoFeedEnableOut(config.getBoolean("client.info_feed.is_on", true)))
            composed(UserCurrency(player))
            composed(UserAchievementScore(player))
            val firstToday = LoginCalendar.isFirstOfDay(profile.lastOnlineAt, now())
            typed(IsFirstLoginOfDayOutCodec, IsFirstLoginOfDayOut(firstToday))
            composed(
                MysteryBoxKeys(
                    config.getValue("mystery_box.tracker.box_colour", ""),
                    config.getValue("mystery_box.tracker.key_colour", ""),
                ),
            )
        }
    }

    private inner class Sends(
        private val client: ClientConnection,
    ) {
        fun composed(message: OutgoingMessage<*>) = client.send(message)

        fun <T> typed(
            codec: OutgoingCodec<T>,
            message: T,
        ) = client.send(codec, message)
    }

    companion object {
        fun unseenItems(tracker: UnseenItems?): AddRoomItem {
            val byCategory = TreeMap<Int, List<Int>>(tracker?.snapshot().orEmpty())
            byCategory.putIfAbsent(AddRoomItem.UnseenCategory.EARNINGS.number, listOf(1))
            return AddRoomItem.byCategoryNumber(byCategory)
        }
    }
}
