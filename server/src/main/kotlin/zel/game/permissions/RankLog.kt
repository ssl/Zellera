package zel.game.permissions

import zel.db.Sql
import zel.db.orFallback
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class RankLog internal constructor(
    private val sql: Sql,
    private val now: () -> Int,
) {
    fun write(
        by: PermissionActor,
        playerId: Int,
        from: Int,
        to: Int,
    ) {
        LOG.orFallback<Unit>("auditing the rank change of player $playerId", Unit) {
            sql.write(INSERT) {
                value(playerId)
                value(from)
                value(to)
                value(now())
                value(by.id.takeUnless { it == PermissionActor.SYSTEM_ID })
                value(by.via.orEmpty())
            }
        }
    }

    private companion object {
        val LOG: Logger = LoggerFactory.getLogger(RankLog::class.java)

        const val INSERT =
            "INSERT INTO rank_changes (player_id, old_rank, new_rank, changed_at, actor_id, via) " +
                "VALUES (?, ?, ?, ?, ?, ?)"
    }
}
