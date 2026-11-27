package zel.protocol.navigator

import zel.game.navigation.Navigator
import zel.game.players.Player
import zel.protocol.generated.GetOfficialRoomsInCodec
import zel.protocol.generated.GuestRoomSearchResultOutCodec
import zel.protocol.generated.GuildBaseSearchInCodec
import zel.protocol.generated.MyFavouriteRoomsSearchInCodec
import zel.protocol.generated.MyFrequentRoomHistorySearchInCodec
import zel.protocol.generated.MyFriendsRoomsSearchInCodec
import zel.protocol.generated.MyGuildBasesSearchInCodec
import zel.protocol.generated.MyRecommendedRoomsInCodec
import zel.protocol.generated.MyRoomHistorySearchInCodec
import zel.protocol.generated.MyRoomRightsSearchInCodec
import zel.protocol.generated.MyRoomsSearchInCodec
import zel.protocol.generated.NavigatorSearchResultBlocksOutCodec
import zel.protocol.generated.NewNavigatorSearchInCodec
import zel.protocol.generated.OfficialRoomsOutCodec
import zel.protocol.generated.PopularRoomsSearchInCodec
import zel.protocol.generated.RoomAdSearchInCodec
import zel.protocol.generated.RoomTextSearchInCodec
import zel.protocol.generated.RoomsWhereMyFriendsAreSearchInCodec
import zel.protocol.generated.RoomsWithHighestScoreSearchInCodec
import zel.protocol.typed.ClientCooldown
import zel.protocol.typed.PacketPackage
import zel.protocol.typed.PacketRoute
import zel.protocol.typed.PacketSession
import zel.protocol.typed.player
import zel.room.listing.ListedRoom
import zel.room.listing.RoomListings

class RoomSearchPackets internal constructor(
    private val rooms: RoomListings,
    private val searches: RoomSearches,
    search: NavigatorSearch,
    navigator: Navigator,
) : PacketPackage {
    override val name = "roomsearches"

    private val textCooldown = ClientCooldown("navigator.textsearch", TEXT_GAP_MS)
    private val navigatorCooldown = ClientCooldown("navigator.search", NAVIGATOR_GAP_MS)

    override val routes =
        listOf(
            list(MyRoomsSearchInCodec, GuestRoomSearch.MY_ROOMS) { rooms.ofOwner(it.profile.id) },
            list(MyFavouriteRoomsSearchInCodec, GuestRoomSearch.FAVOURITES) { rooms.favourites(it.profile.id) },
            list(MyFriendsRoomsSearchInCodec, GuestRoomSearch.FRIENDS_ROOMS, rooms::friendsOwn),
            list(RoomsWhereMyFriendsAreSearchInCodec, GuestRoomSearch.WHERE_FRIENDS_ARE, rooms::friendsNow),
            list(MyRoomRightsSearchInCodec, GuestRoomSearch.ROOM_RIGHTS) { rooms.withRights(it.profile.id) },
            list(MyGuildBasesSearchInCodec, GuestRoomSearch.MY_GUILD_BASES) { rooms.inGroups(it.profile.id) },
            list(MyRoomHistorySearchInCodec, GuestRoomSearch.HISTORY, searches::visited),
            list(
                MyFrequentRoomHistorySearchInCodec,
                GuestRoomSearch.FREQUENT_HISTORY,
                searches::frequentlyVisited,
            ),
            list(MyRecommendedRoomsInCodec, GuestRoomSearch.RECOMMENDED, searches::recommended),
            list(RoomsWithHighestScoreSearchInCodec, GuestRoomSearch.HIGHEST_SCORE) {
                searches.highestScore()
            },
            list(GuildBaseSearchInCodec, GuestRoomSearch.GUILD_BASES) { searches.guildBases() },
            PacketRoute(PopularRoomsSearchInCodec) { session, m ->
                val found = rooms.visibleTo(session.player, searches.popular(m.query))
                session.send(
                    GuestRoomSearchResultOutCodec,
                    NavigatorMessages.guestRooms(GuestRoomSearch.POPULAR, m.query, found),
                )
            },
            PacketRoute(RoomAdSearchInCodec) { session, m ->
                val found = rooms.visibleTo(session.player, searches.runningEvents())
                session.send(GuestRoomSearchResultOutCodec, NavigatorMessages.guestRooms(m.searchType, "", found))
            },
            PacketRoute(RoomTextSearchInCodec) { session, m ->
                if (textCooldown.acquire(session.client)) sendTextSearch(session, m.text)
            },
            PacketRoute(GetOfficialRoomsInCodec) { session, _ ->
                session.send(
                    OfficialRoomsOutCodec,
                    NavigatorMessages.officialRooms(
                        navigator.publicFolders.values
                            .sortedBy { it.order }
                            .map { NavigatorMessages.PublicFolder(it.name, navigator.roomsOf(it)) },
                        rooms.visibleTo(session.player, rooms.promoted()),
                    ),
                )
            },
            PacketRoute(NewNavigatorSearchInCodec) { session, m ->
                if (navigatorCooldown.acquire(session.client)) {
                    search.answer(session.player, m.view, m.query)?.let {
                        session.send(
                            NavigatorSearchResultBlocksOutCodec,
                            NavigatorMessages.searchBlocks(it.code, it.text, it.blocks),
                        )
                    }
                }
            },
        )

    private fun <T> list(
        codec: zel.protocol.generated.IncomingCodec<T>,
        searchType: Int,
        search: (Player) -> List<ListedRoom>,
    ) = PacketRoute(codec) { session, _ ->
        val found = rooms.visibleTo(session.player, search(session.player))
        session.send(GuestRoomSearchResultOutCodec, NavigatorMessages.guestRooms(searchType, "", found))
    }

    private fun sendTextSearch(
        session: PacketSession,
        sent: String,
    ) {
        val text = sent.take(NavigatorTextLimit.SEARCH.max)
        // always search type 8 with the text exactly as sent, prefix included
        val found = rooms.visibleTo(session.player, searches.text(RoomSearches.parse(text)))
        session.send(GuestRoomSearchResultOutCodec, NavigatorMessages.guestRooms(GuestRoomSearch.TEXT, text, found))
    }

    private companion object {
        const val TEXT_GAP_MS = 500L
        const val NAVIGATOR_GAP_MS = 200L
    }
}
