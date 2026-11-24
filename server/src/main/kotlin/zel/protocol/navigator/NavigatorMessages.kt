package zel.protocol.navigator

import zel.game.navigation.NavigatorBlock
import zel.game.navigation.NavigatorSavedSearch
import zel.game.players.PlayerSettings.NavigatorWindow
import zel.protocol.generated.GuestRoomSearchResultOut
import zel.protocol.generated.NavigatorSavedSearchesOut
import zel.protocol.generated.NavigatorSearchResultBlocksOut
import zel.protocol.generated.NewNavigatorPreferencesOut
import zel.protocol.generated.OfficialRoomsOut
import zel.protocol.generated.PacketWriter
import zel.protocol.generated.PlayerFlatCatsOut
import zel.room.listing.ListedRoom
import zel.room.listing.RoomCategory

object NavigatorMessages {
    private const val ENTRY_FOLDER = 4
    private const val ENTRY_ROOM = 2
    private const val TOP_LEVEL = 0
    private const val PROMOTED_CODE = "promoted"

    data class PublicFolder(
        val name: String,
        val rooms: List<ListedRoom>,
    )

    /** Numbered from 1 across folders and rooms. */
    private sealed interface Line {
        fun writeTo(out: PacketWriter)

        class Folder(
            private val no: Int,
            private val name: String,
        ) : Line {
            override fun writeTo(out: PacketWriter) {
                out
                    .int(no)
                    .string(name)
                    .string("")
                    .int(0)
                    .string("")
                    .string("")
                    .int(TOP_LEVEL)
                    .int(0)
                    .int(ENTRY_FOLDER)
                    .boolean(true)
            }
        }

        class Room(
            private val no: Int,
            private val folderNo: Int,
            private val room: ListedRoom,
        ) : Line {
            override fun writeTo(out: PacketWriter) {
                out
                    .int(no)
                    .string(room.name)
                    .string(room.description)
                    .int(1)
                    .string("")
                    .string("")
                    .int(folderNo)
                    .int(room.userCount)
                    .int(ENTRY_ROOM)
                room.writeTo(out)
            }
        }
    }

    fun guestRooms(
        searchType: Int,
        text: String,
        rooms: List<ListedRoom>,
    ): GuestRoomSearchResultOut {
        val out = PacketWriter().int(rooms.size)
        rooms.forEach { it.writeTo(out) }
        out.boolean(false)
        return GuestRoomSearchResultOut(searchType, text, out.toByteArray())
    }

    fun officialRooms(
        folders: List<PublicFolder>,
        promoted: List<ListedRoom>,
    ): OfficialRoomsOut {
        val lines = linesOf(folders)
        val out = PacketWriter().int(lines.size)
        lines.forEach { it.writeTo(out) }
        out.int(0)
        if (promoted.isEmpty()) {
            out.int(0)
        } else {
            out
                .int(1)
                .string(PROMOTED_CODE)
                .string("")
                .int(promoted.size)
            promoted.forEach { it.writeTo(out) }
        }
        return OfficialRoomsOut(out.toByteArray())
    }

    private fun linesOf(folders: List<PublicFolder>): List<Line> =
        buildList {
            folders.filter { it.rooms.isNotEmpty() }.forEach { folder ->
                val folderNo = size + 1
                add(Line.Folder(folderNo, folder.name))
                folder.rooms.forEach { add(Line.Room(size + 1, folderNo, it)) }
            }
        }

    fun searchBlocks(
        code: String,
        text: String,
        blocks: List<NavigatorBlock>,
    ): NavigatorSearchResultBlocksOut {
        val out = PacketWriter().int(blocks.size)
        blocks.forEach { it.writeTo(out) }
        return NavigatorSearchResultBlocksOut(code, text, out.toByteArray())
    }

    fun windowPreferences(window: NavigatorWindow): NewNavigatorPreferencesOut =
        NewNavigatorPreferencesOut(
            window.x,
            window.y,
            window.width,
            window.height,
            window.searchesOpen,
            window.resultsMode,
        )

    fun savedSearches(saved: List<NavigatorSavedSearch>): NavigatorSavedSearchesOut =
        NavigatorSavedSearchesOut(
            saved.map {
                NavigatorSavedSearchesOut.SearchesItem(it.id, it.searchCode.orEmpty(), it.filter.orEmpty(), "")
            },
        )

    fun flatCategories(flat: List<RoomCategory>): PlayerFlatCatsOut =
        PlayerFlatCatsOut(
            flat.map {
                PlayerFlatCatsOut.CategoriesItem(
                    it.id,
                    it.name,
                    it.isVisible,
                    it.isAutomatic,
                    it.automaticCategoryKey,
                    it.globalCategoryKey,
                    it.isStaffOnly,
                )
            },
        )
}
