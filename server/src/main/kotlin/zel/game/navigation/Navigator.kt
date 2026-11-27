package zel.game.navigation

import org.slf4j.LoggerFactory
import zel.core.Configuration
import zel.db.Database
import zel.db.SqlFailure
import zel.game.players.Player
import zel.room.listing.ListedRoom
import zel.room.listing.RoomListings
import zel.server.GameServices

class Navigator(
    services: GameServices,
    private val rooms: RoomListings,
) {
    private val config: Configuration = services.config
    private val database: Database = services.database

    /** A load swaps in a complete map. */
    @Volatile
    var publicFolders: Map<Int, PublicRoomFolder> = emptyMap()
        internal set

    val landing: LandingView = LandingView(this, rooms, config)

    val views: Map<String, NavigatorView> =
        listOf(
            OfficialView(this),
            landing,
            SingleBlockView.roomEvents(this),
            MyWorldView(this),
            SingleBlockView.favourites(this),
        ).associateBy { it.name }

    private class Block(
        val ranked: Boolean = true,
        val rooms: Navigator.(Player) -> List<ListedRoom>,
    )

    private val blocks: Map<String, Block> =
        mapOf(
            "my" to Block { rooms.ofOwner(it.profile.id) },
            "favorites" to Block { rooms.favourites(it.profile.id) },
            "history" to Block(ranked = false) { rooms.visited(it.profile.id, LISTED_ROOMS) },
            "history_freq" to
                Block(ranked = false) { rooms.frequentlyVisited(it.profile.id, it.profile.lastOnlineAt, LISTED_ROOMS) },
            "my_groups" to Block { rooms.inGroups(it.profile.id, LISTED_ROOMS) },
            "with_rights" to Block { rooms.withRights(it.profile.id) },
            "official-root" to Block { rooms.publicRooms() },
            "popular" to Block { rooms.popular(popularAmount()) },
            "hot" to Block(ranked = false) { rooms.visibleTo(it, hottest(popularAmount())) },
            "recommended" to Block { rooms.staffPicked() },
            "categories" to Block { rooms.promoted() },
            "with_friends" to Block { rooms.visibleTo(it, rooms.friendsNow(it, LISTED_ROOMS)) },
            "highest_score" to Block { rooms.visibleTo(it, rooms.topRated(LISTED_ROOMS)) },
        )

    fun load() {
        try {
            publicFolders = readPublicFolders()
        } catch (failed: SqlFailure) {
            LOGGER.error("public room folders not read", failed)
        }
        val staffFolder = publicFolders[config.getInt("navigator.staff_picks.folder_id")]
        rooms.staffPicked().forEach { staffFolder?.add(it.id) }
    }

    private fun readPublicFolders(): Map<Int, PublicRoomFolder> {
        val folders =
            database.queries
                .select("SELECT * FROM public_room_categories WHERE visible = TRUE ORDER BY id")
                .rows(::PublicRoomFolder)
                .associateByTo(LinkedHashMap()) {
                    it.id
                }
        val listed =
            database.queries.select("SELECT * FROM public_rooms WHERE visible = TRUE").rows {
                it.getInt("category_id") to
                    it.getInt("room_id")
            }
        listed.forEach { (folderId, roomId) -> folders[folderId]?.add(roomId) }
        return folders
    }

    private fun popularAmount() = config.getInt("navigator.popular.max_rooms")

    fun hottest(max: Int): List<ListedRoom> =
        rooms
            .byScore()
            .filter { it.userCount > 0 && !it.isPublicRoom }
            .take(max)

    fun roomsOf(category: PublicRoomFolder): List<ListedRoom> = rooms.rooms(category.roomIds)

    fun blockRooms(
        category: String,
        player: Player,
    ): List<ListedRoom>? {
        val block = blocks[category] ?: return null
        val listed = block.rooms(this, player)
        return if (block.ranked) listed.sorted() else listed
    }

    companion object {
        private const val LISTED_ROOMS = 25
        private val LOGGER = LoggerFactory.getLogger(Navigator::class.java)
    }
}
