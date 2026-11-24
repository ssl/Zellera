package zel.room.persist

import zel.db.Sql
import zel.db.intOrNone
import zel.room.DoorMode
import zel.room.RoomIdle
import zel.room.RoomInfo
import zel.room.RoomModeration
import zel.room.RoomVisuals
import zel.room.listing.RoomListings
import java.sql.ResultSet
import javax.sql.DataSource

class RoomStore(
    dataSource: DataSource,
) {
    private val sql = Sql(dataSource)

    fun load(roomId: Int): RoomInfo? = sql.select(SELECT) { int(roomId) }.first(::info)

    fun savePlayerCount(
        roomId: Int,
        players: Int,
    ) {
        sql.write(UPDATE_PLAYERS) {
            int(players)
            int(roomId)
        }
    }

    companion object {
        internal const val SELECT =
            "SELECT r.id, r.owner_id, " + RoomListings.OWNER_NAME + "," +
                " r.name, r.model_name, r.door_secret, r.door_mode, r.max_players, r.guild_id, r.category_id," +
                " r.walk_through, r.hide_walls, r.wall_thickness, r.floor_thickness, r.who_can_mute, r.who_can_kick," +
                " r.who_can_ban, r.chat_protection, r.idle_sleep, r.idle_sleep_seconds," +
                " r.idle_kick, r.idle_kick_seconds, r.is_public, r.is_staff_picked," +
                " r.build_pass_locked, r.build_pass_door_mode" +
                " FROM rooms r LEFT JOIN players u ON u.id = r.owner_id WHERE r.id = ?"

        internal const val UPDATE_PLAYERS = "UPDATE rooms SET player_count = ? WHERE id = ?"

        private fun info(row: ResultSet): RoomInfo =
            RoomInfo(
                id = row.getInt("id"),
                ownerId = row.intOrNone("owner_id"),
                ownerName = row.getString("owner_name"),
                name = row.getString("name"),
                modelName = row.getString("model_name"),
                door = DoorMode.of(row.getString("door_mode"), row.getString("door_secret")),
                maxUsers = row.getInt("max_players"),
                groupId = row.intOrNone("guild_id"),
                categoryId = row.getInt("category_id"),
                allowWalkThrough = row.getBoolean("walk_through"),
                visuals =
                    RoomVisuals(
                        hideWalls = row.getBoolean("hide_walls"),
                        wallThickness = row.getInt("wall_thickness"),
                        floorThickness = row.getInt("floor_thickness"),
                    ),
                moderation =
                    RoomModeration(
                        whoCanMute = row.getInt("who_can_mute"),
                        whoCanKick = row.getInt("who_can_kick"),
                        whoCanBan = row.getInt("who_can_ban"),
                        floodFilter = row.getInt("chat_protection"),
                    ),
                idle =
                    RoomIdle(
                        sleepEnabled = row.getBoolean("idle_sleep"),
                        sleepSeconds = row.getInt("idle_sleep_seconds"),
                        kickEnabled = row.getBoolean("idle_kick"),
                        kickSeconds = row.getInt("idle_kick_seconds"),
                    ),
                isPublic = row.getBoolean("is_public"),
                isStaffPicked = row.getBoolean("is_staff_picked"),
                buildPassDoor =
                    if (row.getBoolean("build_pass_locked")) {
                        DoorMode.of(row.getString("build_pass_door_mode").orEmpty(), row.getString("door_secret"))
                    } else {
                        null
                    },
            )
    }
}
