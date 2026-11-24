package zel.room.rights

import zel.db.Params
import zel.db.ParentRows
import zel.db.Sql
import zel.db.allOrNothing
import zel.db.intOrNone
import zel.db.write
import zel.db.writeEach
import zel.room.DoorMode
import zel.room.entry.NamedPlayer
import zel.room.listing.RoomListings
import java.sql.ResultSet
import javax.sql.DataSource

@Suppress("TooManyFunctions") // one statement per question
class JdbcRightsStore(
    private val dataSource: DataSource,
) : RightsStore {
    private val sql = Sql(dataSource)

    override fun hasRights(
        roomId: Int,
        playerId: Int,
    ): Boolean = sql.select(HAS_RIGHTS, roomId, playerId).first { true } ?: false

    override fun addRights(
        roomId: Int,
        playerId: Int,
    ) {
        sql.write(ADD_RIGHTS, roomId, playerId)
    }

    override fun removeRights(
        roomId: Int,
        playerId: Int,
    ) {
        sql.write(REMOVE_RIGHTS, roomId, playerId)
    }

    override fun playerName(playerId: Int): String? = sql.select(PLAYER_NAME, playerId).first { it.getString(1) }

    override fun rightsHolders(roomId: Int): List<NamedPlayer> = sql.select(RIGHTS_HOLDERS, roomId).rows(::named)

    /** One REPLACE: a ban that fails to save leaves the one before it in place. */
    override fun ban(
        roomId: Int,
        playerId: Int,
        ends: Int,
    ) = ParentRows.unlessGone { sql.write(BAN, roomId, playerId, ends) }

    override fun unban(
        roomId: Int,
        playerId: Int,
    ) {
        sql.write(UNBAN, roomId, playerId)
    }

    override fun bans(
        roomId: Int,
        nowSeconds: Int,
    ): List<NamedPlayer> = sql.select(BANS, roomId, nowSeconds).rows(::named)

    override fun mute(
        roomId: Int,
        playerId: Int,
        ends: Int,
    ) = ParentRows.unlessGone { sql.write(MUTE, roomId, playerId, ends) }

    override fun unmute(
        roomId: Int,
        playerId: Int,
    ) {
        sql.write(UNMUTE, roomId, playerId)
    }

    override fun mutes(
        roomId: Int,
        nowSeconds: Int,
    ): Map<Int, Int> = sql.select(MUTES, roomId, nowSeconds).rows { it.getInt(1) to it.getInt(2) }.toMap()

    override fun raid(roomId: Int): RaidSettings? =
        sql.select(RAID, roomId).first { row ->
            RaidSettings(
                enabled = row.getInt("enabled") != 0,
                sensitivity = row.getInt("sensitivity"),
                actionType = row.getInt("action"),
                banSeconds = row.getInt("ban_seconds"),
                guardEnabled = row.getInt("guard_enabled") != 0,
                guardSeconds = row.getInt("guard_seconds"),
                guardSensitivity = row.getInt("guard_sensitivity"),
            )
        }

    override fun saveRaid(
        roomId: Int,
        settings: RaidSettings,
    ) {
        sql.write(SAVE_RAID) {
            int(roomId)
            boolean(settings.enabled)
            int(settings.sensitivity)
            int(settings.actionType)
            int(settings.banSeconds)
            boolean(settings.guardEnabled)
            int(settings.guardSeconds)
            int(settings.guardSensitivity)
        }
    }

    override fun settings(roomId: Int): RoomSettings? = sql.select(SETTINGS, roomId).first(::settings)

    override fun saveSettings(
        roomId: Int,
        settings: RoomSettings,
    ) {
        dataSource.connection.use { c ->
            c.allOrNothing {
                c.write(SAVE_SETTINGS) {
                    bind(settings)
                    int(roomId)
                }
                c.write(DROP_TAGS) { int(roomId) }
                c.writeEach(ADD_TAG, settings.tags.withIndex()) { (position, tag) ->
                    int(roomId)
                    int(position)
                    string(tag)
                }
            }
        }
    }

    override fun categoryOpenTo(
        categoryId: Int,
        rank: Int,
        staff: Boolean,
    ): Boolean = sql.select(CATEGORY, categoryId, rank, staff).first { true } ?: false

    private fun Params.bind(s: RoomSettings) {
        string(s.name)
        string(s.description)
        string(DoorMode.STORED[s.doorMode])
        string(s.password)
        int(s.maxUsers)
        int(s.categoryId)
        int(s.tradeMode)
        boolean(s.allowPets)
        boolean(s.allowPetsEat)
        boolean(s.allowWalkThrough)
        boolean(s.hideWalls)
        int(s.wallThickness)
        int(s.floorThickness)
        int(s.floodFilter)
        boolean(s.leaveOnDoorTile)
        boolean(s.idleSleep)
        int(s.idleSleepSeconds)
        boolean(s.idleKick)
        int(s.idleKickSeconds)
        boolean(s.muteAllPets)
        int(s.whoCanMute)
        int(s.whoCanKick)
        int(s.whoCanBan)
    }

    private fun settings(row: ResultSet): RoomSettings =
        RoomSettings(
            ownerId = row.intOrNone("owner_id"),
            name = row.getString("name").orEmpty(),
            description = row.getString("description").orEmpty(),
            doorMode = DoorMode.codeOf(row.getString("door_mode")),
            password = row.getString("door_secret").orEmpty(),
            maxUsers = row.getInt("max_players"),
            categoryId = row.getInt("category_id"),
            tags =
                row
                    .getString("tags")
                    .orEmpty()
                    .split(';')
                    .filter { it.isNotEmpty() },
            tradeMode = row.getInt("trade_mode"),
            allowPets = row.getBoolean("pets_allowed"),
            allowPetsEat = row.getBoolean("pets_may_eat"),
            allowWalkThrough = row.getBoolean("walk_through"),
            hideWalls = row.getBoolean("hide_walls"),
            wallThickness = row.getInt("wall_thickness"),
            floorThickness = row.getInt("floor_thickness"),
            floodFilter = row.getInt("chat_protection"),
            leaveOnDoorTile = row.getBoolean("leave_on_door_tile"),
            idleSleep = row.getBoolean("idle_sleep"),
            idleSleepSeconds = row.getInt("idle_sleep_seconds"),
            idleKick = row.getBoolean("idle_kick"),
            idleKickSeconds = row.getInt("idle_kick_seconds"),
            muteAllPets = row.getBoolean("pets_muted"),
            whoCanMute = row.getInt("who_can_mute"),
            whoCanKick = row.getInt("who_can_kick"),
            whoCanBan = row.getInt("who_can_ban"),
        )

    private fun named(row: ResultSet): NamedPlayer = NamedPlayer(row.getInt(1), row.getString(2).orEmpty())

    companion object {
        internal const val HAS_RIGHTS = "SELECT 1 FROM room_rights WHERE room_id = ? AND player_id = ? LIMIT 1"
        internal const val ADD_RIGHTS = "INSERT IGNORE INTO room_rights (room_id, player_id) VALUES (?, ?)"
        internal const val REMOVE_RIGHTS = "DELETE FROM room_rights WHERE room_id = ? AND player_id = ?"
        internal const val PLAYER_NAME = "SELECT name FROM players WHERE id = ?"
        internal const val RIGHTS_HOLDERS =
            "SELECT r.player_id, u.name FROM room_rights r LEFT JOIN players u ON u.id = r.player_id" +
                " WHERE r.room_id = ?"
        internal const val BAN = "REPLACE INTO room_bans (room_id, player_id, ends_at) VALUES (?, ?, ?)"
        internal const val UNBAN = "DELETE FROM room_bans WHERE room_id = ? AND player_id = ?"
        internal const val BANS =
            "SELECT b.player_id, u.name FROM room_bans b LEFT JOIN players u ON u.id = b.player_id" +
                " WHERE b.room_id = ? AND b.ends_at > ? ORDER BY b.ends_at"
        internal const val MUTE = "REPLACE INTO room_mutes (room_id, player_id, ends_at) VALUES (?, ?, ?)"
        internal const val UNMUTE = "DELETE FROM room_mutes WHERE room_id = ? AND player_id = ?"
        internal const val MUTES = "SELECT player_id, ends_at FROM room_mutes WHERE room_id = ? AND ends_at > ?"
        internal const val RAID =
            "SELECT enabled, sensitivity, action, ban_seconds, guard_enabled, guard_seconds," +
                " guard_sensitivity FROM room_raid_settings WHERE room_id = ?"
        internal const val SAVE_RAID =
            "REPLACE INTO room_raid_settings (room_id, enabled, sensitivity, action," +
                " ban_seconds, guard_enabled, guard_seconds, guard_sensitivity)" +
                " VALUES (?, ?, ?, ?, ?, ?, ?, ?)"
        internal const val SETTINGS =
            "SELECT r.owner_id, r.name, r.description, r.door_mode, r.door_secret, r.max_players, r.category_id," +
                " r.trade_mode, r.pets_allowed, r.pets_may_eat, r.walk_through, r.hide_walls, r.wall_thickness," +
                " r.floor_thickness, r.chat_protection, r.leave_on_door_tile, r.idle_sleep," +
                " r.idle_sleep_seconds, r.idle_kick, r.idle_kick_seconds, r.pets_muted," +
                " r.who_can_mute, r.who_can_kick, r.who_can_ban, " + RoomListings.TAGS + " FROM rooms r WHERE r.id = ?"
        internal const val SAVE_SETTINGS =
            "UPDATE rooms SET name = ?, description = ?, door_mode = ?, door_secret = ?, max_players = ?," +
                " category_id = ?, trade_mode = ?, pets_allowed = ?, pets_may_eat = ?, walk_through = ?," +
                " hide_walls = ?, wall_thickness = ?, floor_thickness = ?, chat_protection = ?," +
                " leave_on_door_tile = ?, idle_sleep = ?, idle_sleep_seconds = ?," +
                " idle_kick = ?, idle_kick_seconds = ?, pets_muted = ?, who_can_mute = ?," +
                " who_can_kick = ?, who_can_ban = ? WHERE id = ?"
        internal const val DROP_TAGS = "DELETE FROM room_tags WHERE room_id = ?"
        internal const val ADD_TAG = "INSERT INTO room_tags (room_id, position, tag) VALUES (?, ?, ?)"
        internal const val CATEGORY =
            "SELECT 1 FROM room_categories WHERE id = ? AND min_rank <= ? AND (staff_only = 0 OR ?) LIMIT 1"
    }
}
