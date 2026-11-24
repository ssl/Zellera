package zel.game.players

import zel.db.Database
import zel.db.SqlFailure
import org.slf4j.LoggerFactory

object UserIgnores {
    private val LOG = LoggerFactory.getLogger(UserIgnores::class.java)
    private const val IGNORED_SQL = "SELECT 1 FROM player_ignores WHERE player_id = ? AND ignored_id = ? LIMIT 1"

    fun ignores(
        players: Players,
        database: Database,
        playerId: Int,
        ignoredId: Int,
    ): Boolean {
        players.find(playerId)?.let { return it.stats.contacts.userIgnored(ignoredId) }
        return try {
            database.queries
                .select(IGNORED_SQL, playerId, ignoredId)
                .first { true } ?: false
        } catch (failure: SqlFailure) {
            LOG.error("ignore list of player {} not read", playerId, failure)
            false
        }
    }
}
