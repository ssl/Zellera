package zel.room.protocol

import zel.game.players.Player
import zel.protocol.Outbound
import zel.protocol.generated.AddFavouriteRoomInCodec
import zel.protocol.generated.DeleteFavouriteRoomInCodec
import zel.protocol.generated.FavouriteChangedOut
import zel.protocol.generated.FavouriteChangedOutCodec
import zel.protocol.generated.GetGuestRoomInCodec
import zel.protocol.generated.GetGuestRoomResultOutCodec
import zel.protocol.generated.NavigatorSettingsOut
import zel.protocol.generated.NavigatorSettingsOutCodec
import zel.protocol.generated.UpdateHomeRoomInCodec
import zel.protocol.typed.PacketPackage
import zel.protocol.typed.PacketRoute
import zel.protocol.typed.PacketSession
import zel.room.RoomRegistry
import zel.room.entry.EntryMessages
import zel.room.entry.EntryReader
import zel.room.entry.EntryStore
import zel.room.entry.RoomCard
import zel.room.rights.Moderation
import zel.room.rights.ModerationSetting

class RoomSidePackets(
    private val registry: RoomRegistry,
    private val store: EntryStore,
    private val moderation: Moderation,
    private val nowSeconds: () -> Int,
) : PacketPackage {
    override val name = "room-side"
    private val reader = EntryReader(store)

    override val routes =
        listOf(
            PacketRoute(GetGuestRoomInCodec) { session, m ->
                info(session, m.roomId, enterRoom = m.isLoading == YES, forward = m.checkEntry == YES)
            },
            PacketRoute(AddFavouriteRoomInCodec) { session, m ->
                val player = session.client.player
                if (player != null && store.card(m.roomId) != null && player.stats.favouriteRooms.add(m.roomId)) {
                    session.send(FavouriteChangedOutCodec, FavouriteChangedOut(m.roomId, true))
                }
            },
            PacketRoute(DeleteFavouriteRoomInCodec) { session, m ->
                session.client.player
                    ?.stats
                    ?.favouriteRooms
                    ?.remove(m.roomId)
                session.send(FavouriteChangedOutCodec, FavouriteChangedOut(m.roomId, false))
            },
            PacketRoute(UpdateHomeRoomInCodec) { session, m -> home(session, m.roomId) },
        )

    private fun home(
        session: PacketSession,
        roomId: Int,
    ) {
        val player = session.client.player ?: return
        if (roomId != NO_HOME && store.card(roomId) == null) return
        player.profile.homeRoomId = roomId
        session.send(NavigatorSettingsOutCodec, NavigatorSettingsOut(roomId, 0))
    }

    private fun info(
        session: PacketSession,
        roomId: Int,
        enterRoom: Boolean,
        forward: Boolean,
    ) {
        val player = session.client.player ?: return
        val card = store.card(roomId) ?: return
        session.send(GetGuestRoomResultOutCodec, EntryMessages.roomInfo(card, view(player, card, enterRoom, forward)))
    }

    fun sendInfo(
        player: Player,
        roomId: Int,
        enterRoom: Boolean,
        forward: Boolean,
    ) {
        val card = store.card(roomId) ?: return
        val message = EntryMessages.roomInfo(card, view(player, card, enterRoom, forward))
        player.client?.send(Outbound(GetGuestRoomResultOutCodec, message))
    }

    private fun view(
        player: Player,
        card: RoomCard,
        enterRoom: Boolean,
        forward: Boolean,
    ): EntryMessages.RoomInfoView {
        val level = reader.level(player, card)
        val facts = reader.facts(player.profile.id, card, nowSeconds())
        return EntryMessages.RoomInfoView(
            enterRoom = enterRoom,
            forward = forward,
            userCount = registry.playersIn(card.id).size,
            group = facts.group,
            promotion = facts.promotion,
            isGroupMember = facts.isGroupMember,
            allMuted = registry.withRoom(card.id) { moderation.allMuted(it) } == true,
            canMute = ModerationSetting.allows(card.whoCanMute, level, card.groupId),
        )
    }

    private companion object {
        const val YES = 1

        /** The home room id that clears the home room. */
        const val NO_HOME = 0
    }
}
