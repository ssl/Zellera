package zel.game.moderation

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import zel.db.Sql
import zel.db.orFallback
import java.sql.ResultSet
import javax.sql.DataSource

/**
 * A ban is always on a player and runs until its end; lifting one moves the end. A ban may name an address or a
 * machine it keeps out as well, whoever logs in from there.
 */
class BanStore(
    dataSource: DataSource,
) {
    private val sql = Sql(dataSource)

    fun insert(ban: PlayerBan): Boolean =
        log.orFallback("ban of player ${ban.playerId}", false) {
            sql.write(INSERT) {
                int(ban.playerId)
                intOrNullWhenNotPositive(ban.issuedBy)
                int(ban.issuedAt)
                int(ban.endsAt)
                string(ban.reason.take(REASON_LENGTH))
                intOrNullWhenNotPositive(ban.topicId)
                int(if (ban.avatarOnly) 1 else 0)
            } == 1
        }

    /** The player's ban that runs longest. */
    fun runningFor(
        playerId: Int,
        now: Int,
    ): PlayerBan? =
        log.orFallback("running ban of player $playerId", null) {
            sql.select(LONGEST_RUNNING, playerId, now).first(::ban)
        }

    fun barsMachine(
        machineId: String,
        now: Int,
    ): Boolean = bars("barred_machine", machineId, now)

    fun barsAddress(
        address: String,
        now: Int,
    ): Boolean = bars("barred_address", address, now)

    private fun bars(
        column: String,
        value: String,
        now: Int,
    ): Boolean =
        log.orFallback("ban by $column", false) {
            sql
                .select("SELECT id FROM bans WHERE $column = ? AND ends_at > ? LIMIT 1", value, now)
                .first { true } == true
        }

    private fun ban(row: ResultSet) =
        PlayerBan(
            playerId = row.getInt("player_id"),
            issuedBy = row.getInt("issued_by"),
            issuedAt = row.getInt("issued_at"),
            endsAt = row.getLong("ends_at").coerceAtMost(Int.MAX_VALUE.toLong()).toInt(),
            reason = row.getString("reason").orEmpty(),
            topicId = row.getInt("topic_id"),
            avatarOnly = row.getInt("avatar_only") != 0,
        )

    private companion object {
        val log: Logger = LoggerFactory.getLogger(BanStore::class.java)
        const val REASON_LENGTH = 200
        const val INSERT =
            "INSERT INTO bans (player_id, issued_by, issued_at, ends_at, reason, topic_id, avatar_only) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)"
        const val LONGEST_RUNNING =
            "SELECT player_id, issued_by, issued_at, ends_at, reason, topic_id, avatar_only FROM bans " +
                "WHERE player_id = ? AND ends_at > ? ORDER BY ends_at DESC LIMIT 1"
    }
}
