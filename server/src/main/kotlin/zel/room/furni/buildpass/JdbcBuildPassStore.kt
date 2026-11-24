package zel.room.furni.buildpass

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import zel.core.rules.rules
import zel.db.DeadlockRetry
import zel.db.Sql
import zel.db.allOrNothing
import zel.db.first
import zel.db.insertForId
import zel.db.orFallback
import zel.db.write
import zel.room.DoorMode
import zel.room.furni.BuildPass
import zel.util.MILLIS_PER_SECOND
import java.sql.Connection
import java.sql.SQLIntegrityConstraintViolationException
import javax.sql.DataSource

class JdbcBuildPassStore(
    private val dataSource: DataSource,
    private val baseLimit: () -> Int,
    private val nowSeconds: () -> Long = { System.currentTimeMillis() / MILLIS_PER_SECOND },
) : BuildPassStore {
    private val sql = Sql(dataSource)

    override fun account(playerId: Int): BuildPassAccount =
        checkNotNull(
            sql.select(ACCOUNT, playerId, playerId, playerId).first { row ->
                val seconds = (row.getLong("until") - nowSeconds()).coerceIn(0, Int.MAX_VALUE.toLong()).toInt()
                val member = seconds > 0
                val limit = if (member) baseLimit() + row.getInt("bonus") else rules.nonMemberFurniLimit
                BuildPassAccount(
                    playerId,
                    row.getInt("furni"),
                    seconds,
                    limit,
                    if (member) limit else rules.nonMemberMaxFurniLimit,
                )
            },
        ) { "no Build Pass account row for player $playerId" }

    override fun tracked(clientId: Int): BuildPassTracked? =
        sql.select(TRACKED, clientId).first { row ->
            BuildPassTracked(
                row.getInt("player_id"),
                row.getInt("room_id"),
                row.getInt("guild_id"),
                row.getInt("owner_id"),
            )
        }

    override fun groupAdmins(groupId: Int): List<Int> = sql.select(GROUP_ADMINS, groupId).rows { it.getInt(1) }

    /** A deadlock on the client id index retries. */
    override fun create(
        placerId: Int,
        definitionId: Int,
        roomId: Int,
        state: String,
    ): BuildPassRow? =
        log.orFallback("creating a Build Pass furni for player $placerId in room $roomId", null) {
            DeadlockRetry.run { insert(placerId, definitionId, roomId, state) }
        }

    private fun insert(
        placerId: Int,
        definitionId: Int,
        roomId: Int,
        state: String,
    ): BuildPassRow? =
        dataSource.connection.use { c ->
            c.allOrNothing {
                val rowId =
                    checkNotNull(
                        c.insertForId(INSERT_ITEM) {
                            int(placerId)
                            int(roomId)
                            int(definitionId)
                            string(state)
                        },
                    ) { "no id for a new furni" }
                assignClientId(c, rowId, placerId, roomId)?.let { BuildPassRow(rowId, it) }
            }
        }

    /** Tries the next free client id a few times; two placers can take the same one at once. */
    private fun assignClientId(
        c: Connection,
        rowId: Int,
        placerId: Int,
        roomId: Int,
    ): Int? {
        repeat(ASSIGN_TRIES) {
            try {
                c.write(INSERT_TRACKING) {
                    int(rowId)
                    int(placerId)
                    int(roomId)
                    int(BuildPass.FIRST_ID - 1)
                }
                return c.first(CLIENT_ID, { int(rowId) }) { row -> row.getInt(1) }
            } catch (_: SQLIntegrityConstraintViolationException) {
                return@repeat
            }
        }
        log.warn("no free Build Pass client id for furni {} after {} tries", rowId, ASSIGN_TRIES)
        return null
    }

    override fun delete(rowId: Int) {
        dataSource.connection.use { c ->
            c.allOrNothing { DELETES.forEach { c.write(it) { int(rowId) } } }
        }
    }

    override fun ownedRooms(ownerId: Int): List<BuildPassOwnedRoom> =
        sql.select(OWNED_ROOMS, ownerId).rows { row ->
            BuildPassOwnedRoom(
                row.getInt("id"),
                row.getBoolean("build_pass_locked"),
                row.getString("door_mode") == DoorMode.INVISIBLE,
                row.getInt("has_furni") != 0,
            )
        }

    override fun lockRoom(roomId: Int) {
        sql.write(LOCK_ROOM, roomId)
    }

    override fun unlockRoom(roomId: Int) {
        sql.write(UNLOCK_ROOM, roomId)
    }

    private companion object {
        val log: Logger = LoggerFactory.getLogger(JdbcBuildPassStore::class.java)
        const val ASSIGN_TRIES = 5
        const val GROUP_ADMIN_LEVEL = 1

        const val ACCOUNT =
            "SELECT (SELECT COUNT(*) FROM build_pass_furni b" +
                " JOIN furni i ON i.id = b.furni_id AND i.room_id = b.room_id" +
                " WHERE b.player_id = ?) AS furni," +
                " (SELECT COALESCE(MAX(started_at + duration_seconds), 0) FROM player_subscriptions" +
                " WHERE player_id = ? AND kind = 'build_pass' AND active = TRUE) AS until," +
                " (SELECT COALESCE(MAX(build_pass_bonus_furni), 0) FROM player_stats WHERE player_id = ?) AS bonus"
        const val TRACKED =
            "SELECT b.player_id, b.room_id, r.guild_id, r.owner_id FROM build_pass_furni b " +
                "JOIN rooms r ON r.id = b.room_id WHERE b.client_id = ?"
        const val GROUP_ADMINS =
            "SELECT player_id FROM guild_members WHERE guild_id = ? AND level <= $GROUP_ADMIN_LEVEL"
        const val INSERT_ITEM =
            "INSERT INTO furni (player_id, room_id, definition_id, state) VALUES (?, ?, ?, ?)"
        const val INSERT_TRACKING =
            "INSERT INTO build_pass_furni (furni_id, player_id, room_id, client_id)" +
                " SELECT ?, ?, ?, COALESCE(MAX(client_id), ?) + 1 FROM build_pass_furni"
        const val CLIENT_ID = "SELECT client_id FROM build_pass_furni WHERE furni_id = ?"
        val DELETES =
            listOf(
                "DELETE FROM furni WHERE id = ?",
                "DELETE FROM build_pass_furni WHERE furni_id = ?",
                "DELETE FROM wired_spawned_furni WHERE furni_id = ?",
                "DELETE FROM wired_box_picks WHERE box_id = ?",
                "DELETE FROM wired_boxes WHERE furni_id = ?",
                "DELETE FROM wired_link_members WHERE furni_id = ?",
                "DELETE FROM furni_labels WHERE furni_id = ?",
            )
        const val OWNED_ROOMS =
            "SELECT r.id, r.build_pass_locked, r.door_mode, EXISTS (SELECT 1 FROM build_pass_furni b" +
                " JOIN furni i ON i.id = b.furni_id AND i.room_id = b.room_id" +
                " WHERE b.room_id = r.id AND b.player_id = r.owner_id) AS has_furni" +
                " FROM rooms r WHERE r.owner_id = ?"
        const val LOCK_ROOM =
            "UPDATE rooms SET build_pass_door_mode = " +
                "IF(build_pass_locked, build_pass_door_mode, door_mode), door_mode = 'invisible', " +
                "build_pass_locked = TRUE WHERE id = ?"
        const val UNLOCK_ROOM =
            "UPDATE rooms SET door_mode = build_pass_door_mode, build_pass_locked = FALSE " +
                "WHERE id = ? AND build_pass_locked"
    }
}
