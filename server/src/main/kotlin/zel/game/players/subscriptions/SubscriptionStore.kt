package zel.game.players.subscriptions

import zel.db.SqlFailure
import zel.db.insertForId
import zel.server.GameServices
import org.slf4j.LoggerFactory
import java.sql.Connection
import java.sql.SQLException

private val log = LoggerFactory.getLogger(SubscriptionStore::class.java)

private const val READ_SQL =
    "SELECT id, player_id, kind, started_at, duration_seconds, active FROM player_subscriptions WHERE player_id = ?"
private const val INSERT_SQL =
    "INSERT INTO player_subscriptions (player_id, kind, started_at, duration_seconds, active) VALUES (?, ?, ?, ?, ?)"
private const val SAVE_ACTIVE_SQL = "UPDATE `player_subscriptions` SET `active` = ? WHERE `id` = ? LIMIT 1"
private const val SAVE_DURATION_SQL = "UPDATE `player_subscriptions` SET `duration_seconds` = ? WHERE `id` = ? LIMIT 1"

internal class SubscriptionStore(
    private val services: GameServices,
) {
    fun read(playerId: Int): List<SubscriptionRow> =
        try {
            services.database.queries.select(READ_SQL) { int(playerId) }.rows {
                SubscriptionRow(
                    it.getInt("id"),
                    it.getInt("player_id"),
                    Subscription.typeOfStored(it.getString("kind")),
                    it.getInt("started_at"),
                    it.getInt("duration_seconds"),
                    it.getInt("active") == 1,
                )
            }
        } catch (e: SqlFailure) {
            log.error("Reading the subscriptions of player {} failed", playerId, e)
            emptyList()
        }

    fun insert(
        playerId: Int,
        type: String,
        startedAt: Int,
        duration: Int,
    ): Int? =
        try {
            services.database.dataSource.connection.use { connection ->
                insertRow(connection, SubscriptionRow(0, playerId, type, startedAt, duration, true))
            }
        } catch (e: SQLException) {
            log.error("Adding a {} subscription for player {} failed", type, playerId, e)
            null
        }

    private fun insertRow(
        connection: Connection,
        row: SubscriptionRow,
    ): Int? =
        connection.insertForId(INSERT_SQL) {
            int(row.playerId)
            string(Subscription.storedKind(row.type))
            int(row.startedAt)
            int(row.duration)
            int(if (row.active) 1 else 0)
        }

    fun saveActive(
        id: Int,
        active: Boolean,
    ) = update(SAVE_ACTIVE_SQL, if (active) 1 else 0, id)

    fun saveDuration(
        id: Int,
        duration: Int,
    ) = update(SAVE_DURATION_SQL, duration, id)

    private fun update(
        sql: String,
        value: Int,
        id: Int,
    ) {
        try {
            services.database.queries.write(sql) {
                int(value)
                int(id)
            }
        } catch (e: SqlFailure) {
            log.error("Updating subscription {} failed", id, e)
        }
    }
}
