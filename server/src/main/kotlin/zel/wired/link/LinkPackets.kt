package zel.wired.link

import zel.protocol.generated.WiredLinkCreateNetworkInCodec
import zel.protocol.generated.WiredLinkDeleteNetworkInCodec
import zel.protocol.generated.WiredLinkGetConnectionsInCodec
import zel.protocol.generated.WiredLinkOpenInCodec
import zel.protocol.generated.WiredLinkRenameNetworkInCodec
import zel.protocol.generated.WiredLinkSaveConfigurationInCodec
import zel.protocol.typed.PacketPackage
import zel.protocol.typed.PacketRoute
import zel.protocol.typed.PacketSession
import zel.room.Room
import zel.room.RoomRegistry
import zel.room.furni.place.Interactions
import zel.wired.signal.LinkMessages

class LinkPackets(
    private val registry: RoomRegistry,
    dialog: LinkDialog,
) : PacketPackage {
    override val name = "wired-link"
    override val routes =
        listOf(
            PacketRoute(WiredLinkOpenInCodec) { session, m ->
                answer(session, m.furniId, dialog::open)
            },
            PacketRoute(WiredLinkCreateNetworkInCodec) { session, m ->
                answer(session, m.furniId) { dialog.create(it, m.name) }
            },
            PacketRoute(WiredLinkRenameNetworkInCodec) { session, m ->
                answer(session, m.furniId) { dialog.rename(it, m.networkId, m.name) }
            },
            PacketRoute(WiredLinkDeleteNetworkInCodec) { session, m ->
                answer(session, m.furniId) { dialog.delete(it, m.networkId) }
            },
            PacketRoute(WiredLinkGetConnectionsInCodec) { session, m ->
                answer(session, m.furniId) { dialog.connections(it, m.networkId) }
            },
            PacketRoute(WiredLinkSaveConfigurationInCodec) { session, m ->
                answer(session, m.furniId) { dialog.save(it, m.networkId, m.receiveLocal) }
            },
        )

    private fun answer(
        session: PacketSession,
        furniId: Int,
        operation: (LinkPlace) -> List<LinkReply>,
    ) {
        val playerId =
            session.client.player
                ?.profile
                ?.id ?: return
        val place = place(playerId, furniId)
        operation(place).forEach { LinkOut.send(session, place, it) }
    }

    private fun place(
        playerId: Int,
        furniId: Int,
    ): LinkPlace {
        val roomId = registry.roomOf(playerId) ?: return nowhere(playerId, 0, furniId)
        return registry.withRoom(roomId) { scope -> read(scope.room, playerId, furniId) }
            ?: nowhere(playerId, roomId, furniId)
    }

    private fun read(
        room: Room,
        playerId: Int,
        furniId: Int,
    ): LinkPlace {
        val furni = room.furni[furniId]?.takeIf { Interactions.of(it.definition) == LinkMessages.LINK }
        val links =
            room.furni
                .all()
                .filter { Interactions.of(it.definition) == LinkMessages.LINK }
                .map { it.id }
                .toSet()
        return LinkPlace(playerId, room.info.id, furniId, furni != null, furni?.ownerId == playerId, links)
    }

    private fun nowhere(
        playerId: Int,
        roomId: Int,
        furniId: Int,
    ) = LinkPlace(playerId, roomId, furniId, isLink = false, owned = false, roomLinks = emptySet())
}
