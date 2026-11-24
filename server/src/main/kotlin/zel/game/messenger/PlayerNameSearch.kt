package zel.game.messenger

import zel.db.LikePattern
import zel.db.Sql
import zel.db.SqlFailure
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Players whose name starts with a text, sorted by name. A prefix is what the unique key on the name answers as a
 * range read that stops at the limit, so every search is read fresh.
 */
class PlayerNameSearch(
    private val sql: Sql,
    private val connected: ConnectedPlayers,
    private val maxResults: () -> Int,
) {
    fun find(text: String): List<Friend> =
        try {
            sql
                .select(
                    "SELECT id, name, figure, is_online FROM players WHERE name LIKE ? " +
                        "ORDER BY name COLLATE utf8mb4_general_ci LIMIT ${maxResults()}",
                    LikePattern.startingWith(text),
                ).rows { row ->
                    Friend(
                        id = row.getInt("id"),
                        ownerId = 0,
                        username = row.getString("name"),
                        look = row.getString("figure"),
                        connected = connected,
                        lastKnownOnline = row.getInt("is_online") == 1,
                    )
                }
        } catch (failure: SqlFailure) {
            log.error("name search for '{}' not read", text, failure)
            emptyList()
        }

    private companion object {
        val log: Logger = LoggerFactory.getLogger(PlayerNameSearch::class.java)
    }
}
