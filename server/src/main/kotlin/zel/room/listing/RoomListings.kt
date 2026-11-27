package zel.room.listing

import org.slf4j.LoggerFactory
import zel.core.rules.rules
import zel.db.LikePattern
import zel.db.Sql
import zel.db.insertForId
import zel.db.intOrNone
import zel.db.orFallback
import zel.game.navigation.FrequentRooms
import zel.game.navigation.RoomSearchField
import zel.game.navigation.seesHiddenRooms
import zel.game.permissions.Privilege
import zel.game.players.Player
import zel.room.DoorMode
import zel.room.port.RoomPort
import zel.util.SECONDS_PER_MINUTE
import java.sql.ResultSet
import javax.sql.DataSource

@Suppress("TooManyFunctions") // one query per list the navigator has
class RoomListings(
    private val dataSource: DataSource,
    private val live: () -> RoomPort?,
    private val now: () -> Int,
    private val publicsInPopular: () -> Boolean = { false },
) {
    private val sql = Sql(dataSource)

    private class ReadCategories(
        val list: List<RoomCategory>,
        val readAt: Int,
    )

    @Volatile
    private var categories: ReadCategories? = null

    fun room(id: Int): ListedRoom? = rooms(listOf(id)).firstOrNull()

    fun rooms(ids: Collection<Int>): List<ListedRoom> {
        if (ids.isEmpty()) return emptyList()
        val found = selected("WHERE r.id IN (${marks(ids.size)})", ids.toList()).associateBy { it.id }
        return ids.mapNotNull(found::get)
    }

    fun ofOwner(ownerId: Int): MutableList<ListedRoom> =
        selected("WHERE r.owner_id = ? ORDER BY r.id DESC", listOf(ownerId))

    fun ofOwnerName(ownerName: String): MutableList<ListedRoom> =
        selected(
            "WHERE u.name = ? OR (r.owner_id IS NULL AND ? =" +
                " (SELECT `value` FROM settings WHERE `name` = 'room.ownerless.owner_name')) ORDER BY r.id DESC",
            listOf(ownerName, ownerName),
        )

    fun countOwned(ownerId: Int): Int =
        single("SELECT COUNT(*) FROM rooms WHERE owner_id = ?", listOf(ownerId)) { it.getInt(1) } ?: 0

    fun countFurni(
        roomId: Int,
        ownerId: Int,
    ): Int =
        single(
            "SELECT COUNT(*) FROM furni WHERE room_id = ? AND player_id <=> NULLIF(?, 0)",
            listOf(roomId, ownerId),
        ) { it.getInt(1) } ?: 0

    private fun activeIds(): Set<Int> {
        val loaded = live()?.loadedRoomIds().orEmpty()
        return loaded + column("SELECT id FROM rooms WHERE is_public = TRUE OR is_staff_picked = TRUE", emptyList()) +
            column("SELECT room_id FROM public_rooms WHERE visible = TRUE", emptyList())
    }

    private fun active(): List<ListedRoom> = rooms(activeIds())

    fun byScore(): List<ListedRoom> = active().sortedByDescending { it.score }

    fun topRated(max: Int): List<ListedRoom> = selected("ORDER BY r.score DESC, r.id ASC LIMIT ?", listOf(max))

    fun activeInCategory(categoryId: Int): List<ListedRoom> = active().filter { it.category == categoryId }

    fun popularByCategory(max: Int): Map<Int, List<ListedRoom>> =
        active()
            .filterNot { it.isPublicRoom && !publicsInPopular() }
            .sorted()
            .groupBy { it.category }
            .mapValues { (_, rooms) -> rooms.take(max) }

    fun publicRooms(): List<ListedRoom> = selected("WHERE r.is_public = TRUE ORDER BY r.id DESC", emptyList())

    fun staffPicked(): List<ListedRoom> = selected("WHERE r.is_staff_picked = TRUE", emptyList())

    fun promoted(): List<ListedRoom> =
        selected("WHERE r.is_promoted = TRUE AND p.ends_at > ? ", listOf(now())).filter { it.promotion != null }

    fun popular(max: Int): List<ListedRoom> = occupiedOpen().take(max)

    fun popularPadded(): List<ListedRoom> {
        val occupied = occupiedOpen()
        val busy = occupied.mapTo(HashSet()) { it.id }
        val padding =
            selected(
                "WHERE r.door_mode = 'open' AND r.is_public = FALSE ORDER BY r.score DESC, r.id DESC LIMIT ?",
                listOf(POPULAR_EMPTY_PADDING + busy.size),
            ).filterNot { it.id in busy }.take(POPULAR_EMPTY_PADDING)
        return occupied + padding
    }

    private fun occupied(): List<ListedRoom> {
        val port = live() ?: return emptyList()
        return rooms(port.loadedRoomIds().filter { port.userCount(it) > 0 })
    }

    private fun occupiedOpen(): List<ListedRoom> =
        occupied()
            .filter { it.doorCode == DOOR_OPEN && (publicsInPopular() || !it.isPublicRoom) }
            .sortedWith(ListedRoom.BUSIEST_THEN_TOP_SCORED)

    fun friendsOwn(player: Player): List<ListedRoom> {
        val friends =
            player.messenger.friends.keys
                .toList()
        if (friends.isEmpty()) return emptyList()
        return selected("WHERE r.owner_id IN (${marks(friends.size)}) ORDER BY r.id ASC", friends)
    }

    /** The rooms of friends the player may follow there: one who hides where they are is in no room. */
    fun friendsNow(
        player: Player,
        max: Int = Int.MAX_VALUE,
    ): List<ListedRoom> {
        val port = live() ?: return emptyList()
        val followsAnyone = player.hasPermission(Privilege.FRIENDS_FOLLOW_ANY)
        return rooms(
            player.messenger.friends.values
                .filter { it.shownOnline && (followsAnyone || it.allowsFollowing) }
                .mapNotNull { port.roomOf(it.id) }
                .distinct()
                .take(max),
        )
    }

    /** [found] without the invisible rooms [viewer] has no business in. */
    fun visibleTo(
        viewer: Player,
        found: List<ListedRoom>,
    ): List<ListedRoom> =
        RoomSight.visible(found, viewer.profile.id, seesHiddenRooms(viewer)) { ids ->
            column(
                "SELECT room_id FROM room_rights WHERE player_id = ? AND room_id IN (${marks(ids.size)})",
                listOf(viewer.profile.id) + ids,
            ).toSet()
        }

    fun withRights(playerId: Int): List<ListedRoom> =
        selected(
            "INNER JOIN room_rights rr ON rr.room_id = r.id WHERE rr.player_id = ? ORDER BY r.id DESC",
            listOf(playerId),
        )

    fun inGroups(
        playerId: Int,
        max: Int = Int.MAX_VALUE,
    ): List<ListedRoom> =
        selected(
            "INNER JOIN guild_members gm ON gm.guild_id = r.guild_id WHERE gm.player_id = ? AND gm.level < ? " +
                "ORDER BY r.id DESC LIMIT ?",
            listOf(playerId, NOT_A_MEMBER_LEVEL, max),
        )

    fun favourites(playerId: Int): List<ListedRoom> =
        selected(
            "INNER JOIN player_favourite_rooms f ON f.room_id = r.id WHERE f.player_id = ? ORDER BY r.id DESC",
            listOf(playerId),
        )

    fun visited(
        playerId: Int,
        max: Int,
    ): MutableList<ListedRoom> {
        val ids =
            column(
                "SELECT room_id FROM room_visits WHERE player_id = ? AND entered_at >= ? " +
                    "GROUP BY room_id ORDER BY MAX(entered_at) DESC, MAX(id) DESC LIMIT ?",
                listOf(playerId, now() - HISTORY_SECONDS, max),
            )
        return rooms(ids).toMutableList()
    }

    fun frequentlyVisited(
        playerId: Int,
        sessionStart: Int,
        max: Int,
    ): MutableList<ListedRoom> {
        val kept =
            column(
                "SELECT room_id FROM room_visits WHERE player_id = ? AND entered_at < ? GROUP BY room_id " +
                    "HAVING COUNT(*) >= ? ORDER BY MIN(id)",
                listOf(playerId, sessionStart, FrequentRooms.KEPT_VISITS),
            )
        val session =
            column(
                "SELECT room_id FROM room_visits WHERE player_id = ? AND entered_at >= ? " +
                    "GROUP BY room_id ORDER BY MIN(entered_at), MIN(id)",
                listOf(playerId, sessionStart),
            )
        return rooms(FrequentRooms.order(FrequentRooms.kept(kept), session).take(max)).toMutableList()
    }

    fun groupSearch(
        text: String,
        max: Int,
    ): List<ListedRoom> {
        val parts = text.split(":", limit = 2)
        val prefix = parts[0].lowercase().takeIf { parts.size == 2 && it in GROUP_SEARCH_FIELDS }
        val word = (if (prefix != null) parts[1] else text).trim().lowercase()
        val occupied = occupied().filter { word.isEmpty() || occupiedMatches(it, prefix, word) }
        val named = if (word.isEmpty()) emptyList() else roomsOfGroupsNamed(word)
        return (occupied + named).distinctBy { it.id }.sortedWith(ListedRoom.BUSIEST_THEN_BY_NAME).take(max)
    }

    private fun occupiedMatches(
        room: ListedRoom,
        prefix: String?,
        word: String,
    ): Boolean {
        val owner = room.ownerName.lowercase() == word
        val name = room.name.lowercase().contains(word)
        val tag = room.tagList.any { it.lowercase().contains(word) }
        val group = room.guildName.lowercase().contains(word)
        return when (prefix) {
            "owner" -> owner
            "roomname" -> name
            "tag" -> tag
            "group" -> group
            else -> name || tag || group
        }
    }

    private fun roomsOfGroupsNamed(word: String): List<ListedRoom> {
        val exact = selected("WHERE LOWER(g.name) = ?", listOf(word))
        if (!listsPrefixGroups(exact.size)) return exact
        return exact +
            selected(
                "WHERE LOWER(g.name) LIKE ? ORDER BY r.id ASC LIMIT ?",
                listOf(LikePattern.startingWith(word), GROUP_SEARCH_MAX_ROOMS),
            )
    }

    fun withName(text: String): List<ListedRoom> =
        selected(
            "WHERE r.name LIKE ? ORDER BY r.id ASC LIMIT ?",
            listOf(like(text), TEXT_SEARCH_MAX_ROOMS),
        )

    /** Rooms carrying [tag] whole; the column's collation ignores case. */
    fun withTag(tag: String): List<ListedRoom> =
        selected(
            "WHERE r.id IN (SELECT room_id FROM room_tags WHERE tag = ?) ORDER BY r.id ASC LIMIT ?",
            listOf(tag, TEXT_SEARCH_MAX_ROOMS),
        )

    fun matchingAnything(text: String): List<ListedRoom> {
        val field = RoomSearchField.ANYTHING
        return rooms(idsOfQuery(field.query, field.parameter(text)))
    }

    fun ofGroupsNamed(text: String): List<ListedRoom> =
        selected("WHERE g.name LIKE ? ORDER BY r.id ASC LIMIT ?", listOf(like(text), TEXT_SEARCH_MAX_ROOMS))

    fun tags(): Set<String> = active().flatMapTo(sortedSetOf()) { it.tagList }

    fun find(
        field: RoomSearchField,
        value: String,
        categoryId: Int,
        showsHidden: Boolean,
        max: Int,
    ): Map<Int, MutableList<ListedRoom>> {
        val ids = idsOfQuery(field.query, field.parameter(value))
        return rooms(ids)
            .filter {
                (categoryId <= 0 || it.category == categoryId) && (showsHidden || it.doorCode != DOOR_INVISIBLE)
            }.groupByTo(LinkedHashMap()) { it.category }
            .mapValuesTo(LinkedHashMap()) { (_, rooms) -> rooms.take(max).toMutableList() }
    }

    private fun idsOfQuery(
        query: String,
        value: String,
    ): List<Int> = LOGGER.orFallback("room search", emptyList()) { sql.select(query, value).rows { it.getInt("id") } }

    fun modelExists(name: String): Boolean =
        single("SELECT 1 FROM room_models WHERE name = ?", listOf(name)) { true } == true

    fun modelClubOnly(name: String): Boolean? =
        single("SELECT club_only FROM room_models WHERE name = ?", listOf(name)) { it.getBoolean("club_only") }

    fun create(
        owner: Player,
        request: NewRoomRow,
    ): Int? =
        LOGGER.orFallback("creating a room for ${owner.profile.id}", null) {
            dataSource.connection.use { connection ->
                connection.insertForId(INSERT_ROOM) {
                    int(owner.profile.id)
                    string(request.name)
                    string(request.description)
                    string(request.modelName)
                    int(request.maxUsers)
                    int(request.categoryId)
                    int(request.tradeMode)
                    int(if (rules.newRoomKickSeconds > 0) 1 else 0)
                    int(rules.newRoomKickSeconds)
                    int(now())
                }
            }
        }

    fun categoriesInOrder(): List<RoomCategory> {
        val held = categories
        if (held != null && now() - held.readAt < CATEGORY_REFRESH_SECONDS) return held.list
        return reloadCategories()
    }

    /** The categories as last read, without a database read; null before the first successful read. */
    fun categoriesAsRead(): List<RoomCategory>? = categories?.list

    fun category(id: Int): RoomCategory? = categoriesInOrder().firstOrNull { it.id == id }

    fun categoryByKey(captionKey: String): RoomCategory? =
        categoriesInOrder().firstOrNull { it.captionKey.equals(captionKey, ignoreCase = true) }

    fun categoriesForRank(rank: Int): List<RoomCategory> = categoriesInOrder().filter { it.minRank <= rank }

    /** A failed read keeps the categories read before, so the next call tries again. */
    fun reloadCategories(): List<RoomCategory> {
        val read: List<RoomCategory>? =
            LOGGER.orFallback("loading room categories", null) { sql.select(SELECT_CATEGORIES).rows(::category) }
        if (read == null) return categories?.list.orEmpty()
        categories = ReadCategories(read, now())
        return read
    }

    private fun category(row: ResultSet) =
        RoomCategory(
            id = row.getInt("id"),
            minRank = row.getInt("min_rank"),
            captionKey = row.getString("caption_key"),
            caption = row.getString("caption"),
            name = row.getString("name"),
            isVisible = row.getBoolean("visible"),
            isAutomatic = row.getBoolean("automatic"),
            automaticCategoryKey = row.getString("automatic_key"),
            isStaffOnly = row.getBoolean("staff_only"),
            maxUserCount = row.getInt("max_players"),
            order = row.getInt("sort_order"),
            isPublic = row.getBoolean("is_public"),
        )

    private fun like(text: String) = LikePattern.containing(text)

    private fun marks(count: Int) = List(count) { "?" }.joinToString(",")

    private fun selected(
        where: String,
        args: List<Any>,
    ): MutableList<ListedRoom> {
        val port = live()
        val loaded = port?.loadedRoomIds().orEmpty()
        return LOGGER
            .orFallback("listing rooms $where", emptyList()) {
                sql.select("$SELECT_ROOMS $where") { args.forEach { value(it) } }.rows { row ->
                    val id = row.getInt("id")
                    val isLoaded = id in loaded
                    room(row, id, if (isLoaded) port?.userCount(id) ?: 0 else 0, isLoaded)
                }
            }.toMutableList()
    }

    private fun room(
        row: ResultSet,
        id: Int,
        users: Int,
        isLoaded: Boolean,
    ) = ListedRoom(
        id,
        ListedSettings(
            ownerId = row.intOrNone("owner_id"),
            ownerName = row.getString("owner_name"),
            name = row.getString("name"),
            description = row.getString("description"),
            doorCode = DoorMode.codeOf(row.getString("door_mode")),
            maxPlayers = row.getInt("max_players"),
            category = row.getInt("category_id"),
            score = row.getInt("score"),
            tagText = row.getString("tags"),
            isPublicRoom = row.getBoolean("is_public"),
            isStaffPicked = row.getBoolean("is_staff_picked"),
            allowsOtherPets = row.getBoolean("pets_allowed"),
        ),
        group(row),
        promotion(row),
        users,
        isLoaded,
    )

    private fun group(row: ResultSet): ListedGroup? =
        row.getString("guild_name")?.let {
            ListedGroup(row.getInt("guild_id"), it, row.getString("guild_badge"))
        }

    private fun promotion(row: ResultSet): ListedPromotion? {
        val end = row.getInt("promo_end")
        if (!row.getBoolean("is_promoted") || end <= now()) return null
        return ListedPromotion(
            row.getString("promo_title"),
            row.getString("promo_description"),
            row.intOrNone("promo_start", NO_START),
            (end - now()) / SECONDS_PER_MINUTE,
            row.getInt("promo_category"),
        )
    }

    private fun column(
        query: String,
        args: List<Any>,
    ): List<Int> =
        LOGGER.orFallback(query, emptyList()) { sql.select(query) { args.forEach { value(it) } }.rows { it.getInt(1) } }

    private fun <T : Any> single(
        query: String,
        args: List<Any>,
        read: (ResultSet) -> T,
    ): T? = LOGGER.orFallback(query, null) { sql.select(query) { args.forEach { value(it) } }.first(read) }

    companion object {
        const val POPULAR_EMPTY_PADDING = 20

        const val TEXT_SEARCH_MAX_ROOMS = 75

        const val GROUP_SEARCH_MAX_ROOMS = 75

        const val HISTORY_SECONDS = 259_200

        /** Groups whose name only starts with the search text are listed while fewer groups have the exact name. */
        internal fun listsPrefixGroups(exactlyNamed: Int): Boolean = exactlyNamed < PREFIX_GROUPS_BELOW

        private const val PREFIX_GROUPS_BELOW = 35
        private val GROUP_SEARCH_FIELDS = setOf("owner", "roomname", "tag", "group")
        private const val NO_START = -1
        private const val DOOR_OPEN = 0
        private const val DOOR_INVISIBLE = 3

        private const val NOT_A_MEMBER_LEVEL = 3

        /** The owner's name of room `r` joined as `u`, or the navigator's owner name for rooms without one. */
        internal const val OWNER_NAME =
            "COALESCE(u.name, (SELECT `value` FROM settings WHERE `name` = 'room.ownerless.owner_name'), '')" +
                " AS owner_name"

        /** The tags of room `r` in order, each closed by `;`. */
        internal const val TAGS =
            "COALESCE((SELECT GROUP_CONCAT(CONCAT(t.tag, ';') ORDER BY t.position SEPARATOR '')" +
                " FROM room_tags t WHERE t.room_id = r.id), '') AS tags"

        private const val SELECT_ROOMS =
            "SELECT r.id, r.owner_id, " + OWNER_NAME + "," +
                " r.name, r.description, r.door_mode, r.max_players, r.category_id, r.score, " + TAGS + "," +
                " r.is_public, r.is_staff_picked, r.pets_allowed, r.guild_id, r.is_promoted," +
                " g.name AS guild_name, g.badge_code AS guild_badge," +
                " p.title AS promo_title, p.description AS promo_description," +
                " COALESCE(p.ends_at, 0) AS promo_end, p.starts_at AS promo_start," +
                " COALESCE(p.category, 0) AS promo_category" +
                " FROM rooms r LEFT JOIN players u ON u.id = r.owner_id LEFT JOIN guilds g ON g.id = r.guild_id" +
                " LEFT JOIN room_promotions p ON p.room_id = r.id"

        private const val INSERT_ROOM =
            "INSERT INTO rooms (owner_id, name, description, model_name, max_players, category_id, trade_mode," +
                " idle_kick, idle_kick_seconds, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"

        private const val CATEGORY_REFRESH_SECONDS = 60
        private const val SELECT_CATEGORIES = "SELECT * FROM room_categories ORDER BY sort_order ASC, id ASC"

        private val LOGGER = LoggerFactory.getLogger(RoomListings::class.java)
    }
}
