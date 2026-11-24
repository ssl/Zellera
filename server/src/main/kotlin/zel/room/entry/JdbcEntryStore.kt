package zel.room.entry

import zel.db.Params
import zel.db.Sql
import zel.db.intOrNone
import zel.room.DoorMode
import zel.room.listing.RoomListings
import zel.room.rights.JdbcRightsStore
import zel.util.SECONDS_PER_MINUTE
import java.sql.ResultSet
import javax.sql.DataSource

@Suppress("TooManyFunctions") // one query per question
class JdbcEntryStore(
    dataSource: DataSource,
) : EntryStore {
    private val sql = Sql(dataSource)

    override fun card(roomId: Int): RoomCard? = sql.select(CARD) { ints(roomId) }.first(::card)

    override fun hasRights(
        roomId: Int,
        playerId: Int,
    ): Boolean = exists(JdbcRightsStore.HAS_RIGHTS, roomId, playerId)

    override fun banEnd(
        roomId: Int,
        playerId: Int,
    ): Int? = sql.select(BAN) { ints(roomId, playerId) }.first { it.getInt(1) }

    override fun deleteBan(
        roomId: Int,
        playerId: Int,
    ) {
        sql.write(DELETE_BAN) { ints(roomId, playerId) }
    }

    override fun group(groupId: Int): RoomGroup? =
        if (groupId <= 0) {
            null
        } else {
            sql.select(GROUP) { ints(groupId) }.first {
                RoomGroup(
                    it.getInt("id"),
                    it.getString("name"),
                    it.getString("badge_code"),
                    it.getBoolean("members_may_decorate"),
                )
            }
        }

    override fun memberLevel(
        groupId: Int,
        playerId: Int,
    ): Int? = sql.select(MEMBER) { ints(groupId, playerId) }.first { it.getInt(1) }

    override fun promotion(
        roomId: Int,
        nowSeconds: Int,
    ): RoomPromotion? =
        sql.select(PROMOTION) { ints(roomId, nowSeconds) }.first {
            RoomPromotion(
                it.getString("title"),
                it.getString("description"),
                (it.getInt("ends_at") - nowSeconds) / SECONDS_PER_MINUTE,
                it.getInt("category"),
            )
        }

    override fun hasVoted(
        roomId: Int,
        playerId: Int,
    ): Boolean = exists(VOTED, playerId, roomId)

    override fun visitedBefore(
        roomId: Int,
        playerId: Int,
    ): Boolean = exists(VISITED, roomId, playerId)

    override fun logEntry(
        roomId: Int,
        playerId: Int,
        nowSeconds: Int,
    ) {
        sql.write(LOG_ENTRY) { ints(roomId, playerId, nowSeconds) }
    }

    override fun openVisitStart(
        roomId: Int,
        playerId: Int,
    ): Int? = sql.select(OPEN_VISIT) { ints(roomId, playerId) }.first { it.getInt(1) }

    override fun logExit(
        roomId: Int,
        playerId: Int,
        nowSeconds: Int,
    ) {
        sql.write(LOG_EXIT) { ints(nowSeconds, roomId, playerId) }
    }

    private fun exists(
        query: String,
        vararg ids: Int,
    ): Boolean = sql.select(query) { ints(*ids) }.first { true } ?: false

    private fun Params.ints(vararg values: Int) = values.forEach { int(it) }

    companion object {
        internal const val CARD =
            "SELECT r.id, r.owner_id, " + RoomListings.OWNER_NAME + "," +
                " r.name, r.description, r.model_name, r.door_secret, r.door_mode, r.max_players, r.trade_mode," +
                " r.score, r.category_id, " + RoomListings.TAGS + "," +
                " r.guild_id, r.pets_allowed, r.is_staff_picked, r.who_can_mute, r.who_can_kick," +
                " r.who_can_ban, r.chat_protection, r.wall_paper, r.floor_paper, r.landscape_paper," +
                " r.build_pass_locked" +
                " FROM rooms r LEFT JOIN players u ON u.id = r.owner_id WHERE r.id = ?"
        internal const val BAN =
            "SELECT MAX(ends_at) FROM room_bans WHERE room_id = ? AND player_id = ? HAVING COUNT(*) > 0"
        internal const val DELETE_BAN = "DELETE FROM room_bans WHERE room_id = ? AND player_id = ?"
        internal const val GROUP = "SELECT id, name, badge_code, members_may_decorate FROM guilds WHERE id = ?"
        internal const val MEMBER = "SELECT level FROM guild_members WHERE guild_id = ? AND player_id = ? LIMIT 1"
        internal const val PROMOTION =
            "SELECT title, description, ends_at, category FROM room_promotions WHERE room_id = ? AND ends_at > ?"
        internal const val VOTED = "SELECT 1 FROM room_votes WHERE player_id = ? AND room_id = ? LIMIT 1"
        internal const val VISITED = "SELECT 1 FROM room_visits WHERE room_id = ? AND player_id = ? LIMIT 1"
        internal const val LOG_ENTRY = "INSERT INTO room_visits (room_id, player_id, entered_at) VALUES (?, ?, ?)"
        internal const val OPEN_VISIT =
            "SELECT entered_at FROM room_visits WHERE room_id = ? AND player_id = ? AND left_at IS NULL " +
                "ORDER BY id DESC LIMIT 1"
        internal const val LOG_EXIT =
            "UPDATE room_visits SET left_at = ? WHERE room_id = ? AND player_id = ? AND left_at IS NULL"

        private fun card(row: ResultSet): RoomCard =
            RoomCard(
                id = row.getInt("id"),
                ownerId = row.intOrNone("owner_id"),
                ownerName = row.getString("owner_name"),
                name = row.getString("name"),
                description = row.getString("description").orEmpty(),
                modelName = row.getString("model_name"),
                door = DoorMode.of(row.getString("door_mode"), row.getString("door_secret").orEmpty()),
                maxUsers = row.getInt("max_players"),
                tradeMode = row.getInt("trade_mode"),
                score = row.getInt("score"),
                categoryId = row.getInt("category_id"),
                tags =
                    row
                        .getString("tags")
                        .orEmpty()
                        .split(';')
                        .filter { it.isNotBlank() },
                groupId = row.intOrNone("guild_id"),
                allowPets = row.getBoolean("pets_allowed"),
                staffPicked = row.getBoolean("is_staff_picked"),
                whoCanMute = row.getInt("who_can_mute"),
                whoCanKick = row.getInt("who_can_kick"),
                whoCanBan = row.getInt("who_can_ban"),
                chatProtection = row.getInt("chat_protection"),
                wallpaper = row.getString("wall_paper").orEmpty(),
                floor = row.getString("floor_paper").orEmpty(),
                landscape = row.getString("landscape_paper").orEmpty(),
                buildPassLocked = row.getBoolean("build_pass_locked"),
            )
    }
}
