package zel.protocol.ambassadors

import zel.game.permissions.SecurityLevel
import zel.protocol.generated.AmbassadorAlertInCodec
import zel.protocol.outgoing.generic.alerts.BubbleAlertMessage
import zel.protocol.typed.PacketPackage
import zel.protocol.typed.PacketRoute

class AmbassadorsPackets : PacketPackage {
    override val name = "ambassadors"

    override val routes =
        listOf(
            PacketRoute(AmbassadorAlertInCodec) { session, m ->
                val sender = checkNotNull(session.client.player)
                val inPublicRoom =
                    sender.rooms
                        .roomOf(sender.profile.id)
                        ?.let { sender.rooms.summary(it)?.isPublic } == true
                if (!SecurityLevel.has(sender, SecurityLevel.EMPLOYEE) &&
                    !(inPublicRoom && SecurityLevel.ambassador(sender))
                ) {
                    session.client.warnings.refused("no right")
                    return@PacketRoute
                }
                val roomId = sender.rooms.roomOf(sender.profile.id)
                if (roomId == null || sender.rooms.roomOf(m.userId) != roomId) return@PacketRoute
                val target =
                    sender.services.game.players
                        ?.find(m.userId) ?: return@PacketRoute
                target.client?.send(BubbleAlertMessage("ambassador.alert.warning"))
            },
        )
}
