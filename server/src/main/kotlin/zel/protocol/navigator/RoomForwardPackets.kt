package zel.protocol.navigator

import zel.core.Configuration
import zel.game.navigation.Navigator
import zel.protocol.generated.ConvertGlobalRoomIdInCodec
import zel.protocol.generated.ForwardToARandomPromotedRoomInCodec
import zel.protocol.generated.ForwardToSomeRoomInCodec
import zel.protocol.generated.NavigatorSettingsOut
import zel.protocol.generated.NavigatorSettingsOutCodec
import zel.protocol.generated.RoomForwardOut
import zel.protocol.generated.RoomForwardOutCodec
import zel.protocol.generated.RoomNetworkOpenConnectionInCodec
import zel.protocol.outgoing.generic.alerts.Broadcast
import zel.protocol.typed.ClientCooldown
import zel.protocol.typed.PacketPackage
import zel.protocol.typed.PacketRoute
import zel.protocol.typed.PacketSession
import zel.protocol.typed.player
import zel.room.listing.RoomListings
import zel.room.port.RoomPort

class RoomForwardPackets(
    private val rooms: RoomListings,
    private val navigator: Navigator,
    private val config: Configuration,
    private val live: () -> RoomPort,
) : PacketPackage {
    override val name = "roomforward"

    private val convertCooldown = ClientCooldown("navigator.convertglobalroomid", CONVERT_GAP_MS)

    override val routes =
        listOf(
            PacketRoute(ConvertGlobalRoomIdInCodec) { session, m ->
                if (convertCooldown.acquire(session.client)) convert(session, m.target)
            },
            PacketRoute(ForwardToSomeRoomInCodec) { session, m -> forward(session, m.data) },
            // Nothing answers it, whatever the string and whether promotions run or not.
            PacketRoute(ForwardToARandomPromotedRoomInCodec) { _, _ -> },
            PacketRoute(RoomNetworkOpenConnectionInCodec) { session, m ->
                joinNetwork(session, m.networkId, m.homeRoomId)
            },
        )

    private fun convert(
        session: PacketSession,
        sent: String,
    ) {
        val target = NavigatorTextLimit.SEARCH.clean(sent)
        if (target.isEmpty()) return
        val roomId =
            target.toIntOrNull()?.takeIf { it > 0 }
                ?: rooms.withName(target).firstOrNull { it.name.equals(target, ignoreCase = true) }?.id
                ?: return
        session.client.send(RoomForwardOutCodec, RoomForwardOut(roomId))
    }

    private fun forward(
        session: PacketSession,
        data: String,
    ) {
        val client = session.client
        when {
            data == "random_friending_room" -> {
                live().loadedRoomIds().randomOrNull()?.let { client.send(RoomForwardOutCodec, RoomForwardOut(it)) }
            }

            data.equals("predefined_noob_lobby", ignoreCase = true) -> {
                client.send(RoomForwardOutCodec, RoomForwardOut(config.getInt("navigator.newcomer_lobby.room_id")))
            }

            else -> {
                val home = session.player.profile.homeRoomId
                client.send(NavigatorSettingsOutCodec, NavigatorSettingsOut(home, home))
            }
        }
    }

    private fun joinNetwork(
        session: PacketSession,
        networkId: Int,
        homeRoomId: Int,
    ) {
        val candidates =
            navigator.publicFolders.values
                .filter { networkId <= 0 || it.id == networkId }
                .flatMap(navigator::roomsOf)
                .filter { it.userCount < it.maxPlayers }
        val target =
            candidates.firstOrNull { it.id == homeRoomId }
                ?: candidates.randomOrNull()
        if (target == null) {
            session.client.send(Broadcast("\${navigator.noroomsfound}"))
        } else {
            session.client.send(RoomForwardOutCodec, RoomForwardOut(target.id))
        }
    }

    private companion object {
        const val CONVERT_GAP_MS = 1000L
    }
}
