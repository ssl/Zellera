package zel.game.players.inventory

import zel.db.SqlFailure
import zel.db.insertForId
import zel.db.writeEach
import zel.game.players.PlayerBadge
import zel.server.GameServices
import org.slf4j.LoggerFactory
import java.sql.Connection
import java.sql.ResultSet
import java.sql.SQLException

private val log = LoggerFactory.getLogger(BadgeStore::class.java)

private const val OWN_BADGES_SQL =
    "SELECT id, badge_code, slot FROM player_badges WHERE player_id = ? ORDER BY badge_code COLLATE utf8mb4_general_ci"
private const val WORN_BADGES_SQL =
    "SELECT id, badge_code, slot FROM player_badges WHERE player_id = ? AND slot BETWEEN 1 AND 5 ORDER BY slot"
private const val DELETE_BADGE_SQL = "DELETE FROM player_badges WHERE player_id = ? AND badge_code = ?"
private const val INSERT_BADGE_SQL = "INSERT INTO player_badges (player_id, badge_code, slot) VALUES (?, ?, ?)"
private const val UPDATE_BADGE_SQL = "UPDATE player_badges SET slot = ?, badge_code = ? WHERE id = ? AND player_id = ?"

internal class BadgeStore(
    private val services: GameServices,
) {
    fun owned(playerId: Int): List<PlayerBadge> =
        orNone(playerId) {
            services.database.queries
                .select(OWN_BADGES_SQL) { int(playerId) }
                .rows(::read)
        }

    fun wornOffline(playerId: Int): List<PlayerBadge> =
        orNone(playerId) {
            services.database.queries
                .select(WORN_BADGES_SQL) { int(playerId) }
                .rows(::read)
        }

    @Throws(SQLException::class)
    fun insert(
        connection: Connection,
        playerId: Int,
        badge: PlayerBadge,
    ) {
        badge.id =
            connection.insertForId(INSERT_BADGE_SQL) {
                int(playerId)
                string(badge.code)
                int(badge.slot)
            } ?: throw SQLException("badge ${badge.code} of player $playerId got no row id")
        services.badgeCaches.invalidate()
    }

    fun insert(
        playerId: Int,
        badge: PlayerBadge,
    ): Boolean =
        try {
            services.database.dataSource.connection
                .use { insert(it, playerId, badge) }
            true
        } catch (e: SQLException) {
            log.error("badge {} of player {} not stored", badge.code, playerId, e)
            false
        }

    fun updateSlots(
        playerId: Int,
        slots: List<BadgeSlot>,
    ) {
        if (slots.isEmpty()) return
        try {
            services.database.dataSource.connection.use { connection ->
                connection.writeEach(UPDATE_BADGE_SQL, slots) {
                    int(it.slot)
                    string(it.code)
                    int(it.id)
                    int(playerId)
                }
            }
        } catch (e: SQLException) {
            log.error("badge slots of player {} not saved", playerId, e)
        }
    }

    fun delete(
        playerId: Int,
        code: String,
    ) {
        try {
            services.database.queries.write(DELETE_BADGE_SQL) {
                int(playerId)
                string(code)
            }
        } catch (e: SqlFailure) {
            log.error("badge {} of player {} not deleted", code, playerId, e)
        }
        services.badgeCaches.invalidate()
        services.game.accountLevels.inputChanged(playerId)
    }

    private fun read(row: ResultSet) = PlayerBadge(row.getInt("id"), row.getString("badge_code"), row.getInt("slot"))

    private fun orNone(
        playerId: Int,
        read: () -> List<PlayerBadge>,
    ): List<PlayerBadge> =
        try {
            read()
        } catch (e: SQLException) {
            log.error("badges of player {} not read", playerId, e)
            emptyList()
        }
}
