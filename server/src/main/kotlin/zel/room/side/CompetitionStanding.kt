package zel.room.side

import zel.game.navigation.NavigatorBlock
import zel.protocol.generated.CompetitionRoomsDataOut
import zel.protocol.generated.NavigatorSearchResultBlocksOut
import zel.protocol.generated.PacketWriter
import zel.protocol.navigator.NavigatorMessages
import zel.room.listing.RoomListings

class CompetitionStanding internal constructor(
    private val store: CompetitionStore,
    private val listings: RoomListings,
) {
    fun page(
        competition: Competition,
        page: Int,
    ): Pair<NavigatorSearchResultBlocksOut, CompetitionRoomsDataOut> {
        val pages = ((store.standingRooms(competition.id) + PAGE_SIZE - 1) / PAGE_SIZE).coerceAtLeast(1)
        val shown = page.coerceIn(0, pages - 1)
        val ids = store.standing(competition.id, shown * PAGE_SIZE, PAGE_SIZE)
        val blocks = NavigatorMessages.searchBlocks(LIST_CODE, competition.code, listOf(roomList(competition, ids)))
        val where =
            PacketWriter()
                .int(competition.id)
                .int(shown)
                .int(pages)
                .toByteArray()
        return blocks to CompetitionRoomsDataOut(where)
    }

    private fun roomList(
        competition: Competition,
        roomIds: List<Int>,
    ) = NavigatorBlock(LIST_CODE, competition.code, listings.rooms(roomIds))

    private companion object {
        const val PAGE_SIZE = 20
        const val LIST_CODE = "competition"
    }
}
