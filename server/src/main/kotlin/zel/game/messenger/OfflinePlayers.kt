package zel.game.messenger

import zel.core.rules.rules
import zel.db.Sql

class OfflinePlayers(
    private val sql: Sql,
) {
    fun byName(name: String): OfflineFriendTarget? = load("p.name = ?", name)

    fun byId(id: Int): OfflineFriendTarget? = load("p.id = ?", id)

    private fun load(
        where: String,
        key: Any,
    ): OfflineFriendTarget? =
        sql.select("$SELECT WHERE $where LIMIT 1", key).first {
            OfflineFriendTarget(
                it.getInt("id"),
                it.getInt("rank_id"),
                it.getBoolean("block_friend_requests"),
                it.getInt("friend_count"),
                it.getInt("max_friends").takeUnless { _ -> it.wasNull() } ?: rules.maxFriends,
            )
        }

    private companion object {
        /** A player who never logged in has no preferences or standing row: they take requests at the base limit. */
        const val SELECT =
            "SELECT p.id, p.rank_id, COALESCE(pr.block_friend_requests, FALSE) AS block_friend_requests, " +
                "st.max_friends, " +
                "(SELECT COUNT(*) FROM friendships f WHERE f.player_id = p.id) AS friend_count FROM players p " +
                "LEFT JOIN player_preferences pr ON pr.player_id = p.id " +
                "LEFT JOIN player_standing st ON st.player_id = p.id"
    }
}
