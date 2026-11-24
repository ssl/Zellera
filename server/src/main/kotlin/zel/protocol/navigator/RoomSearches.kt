package zel.protocol.navigator

import zel.game.navigation.Navigator
import zel.game.players.Player
import zel.room.listing.ListedRoom
import zel.room.listing.RoomListings
import java.util.Locale

internal class RoomSearches(
    private val rooms: RoomListings,
    private val navigator: Navigator,
    private val limits: Limits = Limits(),
) {
    data class Limits(
        val historyRooms: Int = 25,
        val topRatedRooms: Int = 50,
    )

    /** [ANY] is a text without a prefix. */
    enum class Field(
        val prefix: String,
    ) {
        ANY(""),
        NAME("roomname:"),
        OWNER("owner:"),
        TAG("tag:"),
        GROUP("group:"),
    }

    data class RoomText(
        val field: Field,
        val words: String,
    )

    fun highestScore(): List<ListedRoom> = rooms.topRated(limits.topRatedRooms)

    fun visited(player: Player): List<ListedRoom> = rooms.visited(player.profile.id, limits.historyRooms)

    fun frequentlyVisited(player: Player): List<ListedRoom> =
        rooms.frequentlyVisited(player.profile.id, player.profile.lastOnlineAt, limits.historyRooms)

    fun recommended(player: Player): List<ListedRoom> = navigator.blockRooms("recommended", player).orEmpty()

    fun guildBases(): List<ListedRoom> = rooms.groupSearch("", RoomListings.GROUP_SEARCH_MAX_ROOMS)

    fun popular(text: String): List<ListedRoom> = matching(rooms.popularPadded(), text)

    fun runningEvents(): List<ListedRoom> = eventRooms(rooms.promoted())

    fun text(search: RoomText): List<ListedRoom> {
        val words = search.words
        if (words.isEmpty()) return emptyList()
        val found =
            when (search.field) {
                Field.OWNER -> rooms.ofOwnerName(words)
                Field.TAG -> rooms.withTag(words).sorted()
                Field.GROUP -> rooms.ofGroupsNamed(words).sorted()
                Field.ANY -> rooms.matchingAnything(words).sorted()
                Field.NAME -> rooms.withName(words).sorted()
            }
        return found.take(RoomListings.TEXT_SEARCH_MAX_ROOMS)
    }

    companion object {
        const val MAX_EVENT_ROOMS = 20

        fun parse(text: String?): RoomText {
            val clean = NavigatorTextLimit.SEARCH.clean(text)
            val field =
                Field.entries.firstOrNull { it != Field.ANY && clean.startsWith(it.prefix, ignoreCase = true) }
                    ?: return RoomText(Field.ANY, clean)
            return RoomText(field, clean.drop(field.prefix.length).trim())
        }

        fun matching(
            found: List<ListedRoom>,
            text: String,
        ): List<ListedRoom> {
            if (text.isEmpty()) return found
            val needle = text.lowercase(Locale.ROOT)
            return found.filter {
                it.name.lowercase(Locale.ROOT).contains(needle) || it.ownerName.equals(text, ignoreCase = true)
            }
        }

        fun eventRooms(promoted: Collection<ListedRoom>): List<ListedRoom> =
            promoted
                .mapNotNull { room -> room.promotion?.let { room to it.startTimestamp } }
                .sortedWith(compareByDescending<Pair<ListedRoom, Int>> { it.second }.thenBy { it.first.id })
                .take(MAX_EVENT_ROOMS)
                .map { it.first }
    }
}
