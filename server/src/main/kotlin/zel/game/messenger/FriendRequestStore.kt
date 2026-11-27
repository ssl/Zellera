package zel.game.messenger

import org.slf4j.LoggerFactory
import zel.db.Sql
import zel.db.orFallback

class FriendRequestStore(
    private val sql: Sql,
    private val clock: () -> Int,
) {
    /** Which of the two asked the other; both when their requests crossed. */
    data class Between(
        val askers: Set<Int>,
    ) {
        fun askedBy(playerId: Int): Boolean = playerId in askers
    }

    fun between(
        first: Int,
        second: Int,
    ): Between =
        LOGGER.orFallback("friend requests between $first and $second", Between(emptySet())) {
            Between(
                sql
                    .select(BETWEEN) {
                        int(first)
                        int(second)
                        int(second)
                        int(first)
                    }.rows { it.getInt(1) }
                    .toSet(),
            )
        }

    fun isWaiting(
        from: Int,
        to: Int,
    ): Boolean = between(from, to).askedBy(from)

    /** False when the same request already waits. */
    fun add(
        from: Int,
        to: Int,
    ): Boolean =
        LOGGER.orFallback("friend request from $from to $to", false) {
            sql.write(ADD) {
                int(from)
                int(to)
                int(clock())
            } > 0
        }

    /** Requests [playerId] sent that nobody answered yet. */
    fun sentBy(playerId: Int): Int =
        LOGGER.orFallback("friend requests sent by $playerId", COUNT_UNAVAILABLE) {
            sql.select(COUNT_SENT, playerId).first { it.getInt(1) } ?: COUNT_UNAVAILABLE
        }

    fun waitingCount(playerId: Int): Int =
        LOGGER.orFallback("friend requests waiting for $playerId", 0) {
            sql.select(COUNT_WAITING, playerId).first { it.getInt(1) } ?: 0
        }

    /** Oldest first. */
    fun waitingFor(playerId: Int): List<FriendRequest> =
        LOGGER.orFallback("friend requests to $playerId", emptyList()) {
            sql.select(WAITING, playerId).rows {
                FriendRequest(it.getInt("sender_id"), it.getString("name"), it.getString("figure"))
            }
        }

    fun declineAll(playerId: Int) =
        LOGGER.orFallback("declining the friend requests to $playerId", Unit) { sql.write(DECLINE_ALL, playerId) }

    /** Only what the [senders] asked of [to]; what [to] asked of them stays. */
    fun decline(
        senders: Collection<Int>,
        to: Int,
    ) {
        senders.chunked(STATEMENT_IDS).forEach { chunk ->
            LOGGER.orFallback("declining friend requests to $to", Unit) {
                sql.write(DECLINE + chunk.joinToString(",") { "?" } + ")") {
                    int(to)
                    chunk.forEach { int(it) }
                }
            }
        }
    }

    internal companion object {
        const val COUNT_UNAVAILABLE = Int.MAX_VALUE / 2

        private val LOGGER = LoggerFactory.getLogger(FriendRequestStore::class.java)
        private const val BETWEEN =
            "SELECT sender_id FROM friend_requests WHERE (sender_id, target_id) IN ((?, ?), (?, ?))"
        private const val ADD = "INSERT IGNORE INTO friend_requests (sender_id, target_id, sent_at) VALUES (?, ?, ?)"
        private const val COUNT_SENT = "SELECT COUNT(*) FROM friend_requests WHERE sender_id = ?"
        private const val COUNT_WAITING = "SELECT COUNT(*) FROM friend_requests WHERE target_id = ?"
        private const val WAITING =
            "SELECT r.sender_id, p.name, p.figure FROM friend_requests r " +
                "INNER JOIN players p ON p.id = r.sender_id WHERE r.target_id = ? ORDER BY r.sent_at, r.sender_id"
        private const val STATEMENT_IDS = 500
        private const val DECLINE = "DELETE FROM friend_requests WHERE target_id = ? AND sender_id IN ("
        private const val DECLINE_ALL = "DELETE FROM friend_requests WHERE target_id = ?"

        /** Removes the one request of a sender to a target. */
        const val TAKE = "DELETE FROM friend_requests WHERE sender_id = ? AND target_id = ?"
    }
}
