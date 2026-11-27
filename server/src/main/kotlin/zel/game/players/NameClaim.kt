package zel.game.players

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import zel.db.Sql
import zel.db.SqlFailure

/** Names are handed out by the unique key on `players.name`: of two players after one name, one write fails. */
class NameClaim(
    private val sql: Sql,
) {
    fun reservedForOthers(
        name: String,
        playerId: Int,
        since: Int,
    ): Boolean = sql.select(RESERVED, name, playerId, since).first { true } != null

    /** True when [name] now belongs to [playerId]; false when another player holds it or the write failed. */
    fun take(
        playerId: Int,
        name: String,
    ): Boolean =
        try {
            sql.write(CLAIM) {
                string(name)
                int(playerId)
            } > 0
        } catch (e: SqlFailure) {
            if (e.errorCode != DUPLICATE_KEY) log.error("Claiming a name for player {} failed", playerId, e)
            false
        }

    private companion object {
        val log: Logger = LoggerFactory.getLogger(NameClaim::class.java)
        const val DUPLICATE_KEY = 1062
        const val CLAIM = "UPDATE players SET name = ? WHERE id = ?"
        const val RESERVED =
            "SELECT 1 FROM name_changes WHERE old_name = ? AND player_id <> ? AND changed_at >= ? LIMIT 1"
    }
}
