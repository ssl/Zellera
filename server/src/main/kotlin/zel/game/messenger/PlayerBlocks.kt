package zel.game.messenger

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import zel.db.Sql
import zel.db.SqlFailure

/** From the session when the blocker is online, else `player_blocks`; an unreadable list counts as no block. */
class PlayerBlocks(
    private val sql: Sql,
    private val connected: ConnectedPlayers,
) {
    fun blocks(
        blockerId: Int,
        blockedId: Int,
    ): Boolean {
        connected.byId(blockerId)?.let { return it.stats.contacts.userBlocked(blockedId) }
        return try {
            sql.select(BLOCKED, blockerId, blockedId).first { true } ?: false
        } catch (failure: SqlFailure) {
            LOG.error("Block list of player {} not read", blockerId, failure)
            false
        }
    }

    private companion object {
        val LOG: Logger = LoggerFactory.getLogger(PlayerBlocks::class.java)
        const val BLOCKED = "SELECT 1 FROM player_blocks WHERE player_id = ? AND blocked_id = ? LIMIT 1"
    }
}
