package zel.game.navigation

import zel.db.LikePattern
import zel.room.listing.ListedRoom

/** Navigator search fields by prefix (`owner:`, `roomname:`, `tag:`, `group:`); tags match whole, others anywhere. */
enum class RoomSearchField(
    private val prefix: String,
    val query: String,
) {
    ANYTHING(
        "",
        "$ROOMS WHERE CONCAT_WS(' ', r.name, $OWNER_NAME, g.name, " +
            "(SELECT GROUP_CONCAT(t.tag SEPARATOR ' ') FROM room_tags t WHERE t.room_id = r.id)) LIKE ? $ORDER",
    ),
    ROOM_NAME("roomname", "$ROOMS WHERE r.name LIKE ? $ORDER"),
    OWNER("owner", "$ROOMS WHERE $OWNER_NAME LIKE ? $ORDER"),
    TAG("tag", "$ROOMS WHERE EXISTS (SELECT 1 FROM room_tags t WHERE t.room_id = r.id AND t.tag = ?) $ORDER"),
    GROUP("group", "$ROOMS WHERE g.name LIKE ? $ORDER"),
    ;

    fun parameter(value: String): String = if (this == TAG) value else LikePattern.containing(value)

    fun keeps(
        room: ListedRoom,
        value: String,
    ): Boolean =
        value.isEmpty() ||
            when (this) {
                ANYTHING -> room.filterAnything().contains(value, ignoreCase = true)
                ROOM_NAME -> room.name.contains(value, ignoreCase = true)
                OWNER -> room.ownerName.contains(value, ignoreCase = true)
                TAG -> room.tagList.any { it.equals(value, ignoreCase = true) }
                GROUP -> room.guildName.contains(value, ignoreCase = true)
            }

    companion object {
        fun named(prefix: String): RoomSearchField =
            entries.firstOrNull { it.prefix.isNotEmpty() && it.prefix.equals(prefix.trim(), ignoreCase = true) }
                ?: ANYTHING
    }
}

private const val ROOMS =
    "SELECT r.id FROM rooms r LEFT JOIN players p ON p.id = r.owner_id LEFT JOIN guilds g ON g.id = r.guild_id"

/** A room without an owner row is listed under the official account's name (`room.ownerless.owner_name`). */
private const val OWNER_NAME =
    "COALESCE(p.name, (SELECT `value` FROM settings WHERE `name` = 'room.ownerless.owner_name'), '')"
private const val ORDER = "ORDER BY r.name ASC, r.id ASC LIMIT 1000"
