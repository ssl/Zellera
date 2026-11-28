package zel.protocol.misc

import io.netty.util.AttributeKey
import zel.game.achievements.Achievements
import zel.net.clients.ClientConnection
import zel.protocol.generated.BadgePointLimitsOutCodec
import zel.protocol.generated.ClickCharacterInCodec
import zel.protocol.generated.ClientWindowBlurredInCodec
import zel.protocol.generated.ClientWindowFocusedInCodec
import zel.protocol.generated.FriendRequestQuestCompleteInCodec
import zel.protocol.generated.GetBadgePointLimitsInCodec
import zel.protocol.generated.GetInterstitialInCodec
import zel.protocol.generated.GetNftCreditsInCodec
import zel.protocol.generated.GetSilverInCodec
import zel.protocol.generated.IncomingCodec
import zel.protocol.generated.InterstitialOut
import zel.protocol.generated.InterstitialOutCodec
import zel.protocol.generated.LagWarningReportInCodec
import zel.protocol.generated.PerformanceLogInCodec
import zel.protocol.generated.RoomAdEventTabAdClickedInCodec
import zel.protocol.generated.RoomAdEventTabViewedInCodec
import zel.protocol.generated.RoomAdPurchaseInitiatedInCodec
import zel.protocol.generated.SetRoomSessionTagsInCodec
import zel.protocol.generated.SilverBalanceOut
import zel.protocol.generated.SilverBalanceOutCodec
import zel.protocol.generated.VersionCheckInCodec
import zel.protocol.outgoing.inventory.BadgePointLimits
import zel.protocol.typed.PacketPackage
import zel.protocol.typed.PacketRoute

class MiscPackets(
    achievements: () -> Achievements?,
) : PacketPackage {
    override val name = "misc"

    override val routes: List<PacketRoute<*>> =
        listOf(
            PacketRoute(GetInterstitialInCodec) { session, _ ->
                session.send(InterstitialOutCodec, InterstitialOut(false))
            },
            PacketRoute(GetSilverInCodec) { session, _ ->
                session.send(SilverBalanceOutCodec, SilverBalanceOut(0))
            },
            PacketRoute(GetBadgePointLimitsInCodec) { session, _ ->
                achievements()?.let { session.send(BadgePointLimitsOutCodec, BadgePointLimits.of(it)) }
            },
            unanswered(VersionCheckInCodec),
            unanswered(GetNftCreditsInCodec),
            unanswered(LagWarningReportInCodec),
            unanswered(ClickCharacterInCodec),
            unanswered(FriendRequestQuestCompleteInCodec),
            unanswered(RoomAdPurchaseInitiatedInCodec),
            PacketRoute(ClientWindowBlurredInCodec) { session, _ -> ClientWindow.report(session.client, false) },
            PacketRoute(ClientWindowFocusedInCodec) { session, _ -> ClientWindow.report(session.client, true) },
            unanswered(PerformanceLogInCodec),
            unanswered(RoomAdEventTabAdClickedInCodec),
            unanswered(RoomAdEventTabViewedInCodec),
            unanswered(SetRoomSessionTagsInCodec),
        )

    private fun <T> unanswered(codec: IncomingCodec<T>) = PacketRoute(codec) { _, _ -> }
}

/** Whether the client's window has the focus, as it last reported; a client that never reported counts as focused. */
object ClientWindow {
    private val UNFOCUSED: AttributeKey<Boolean> = AttributeKey.valueOf("window-unfocused")

    fun report(
        client: ClientConnection,
        focused: Boolean,
    ) {
        client.channel.attr(UNFOCUSED).set(!focused)
    }

    fun isInBackground(client: ClientConnection): Boolean = client.channel.attr(UNFOCUSED).get() == true
}
