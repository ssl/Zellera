package zel.game.guilds.forums

import zel.db.Sql
import zel.db.orFallback
import zel.game.guilds.guildStorageLog
import zel.game.guilds.readInto
import java.util.concurrent.ConcurrentHashMap
import javax.sql.DataSource

/** Each player's last visit per guild forum within the last week, which ranks the most viewed forums. */
class ForumVisitLog(
    private val dataSource: DataSource,
    private val clock: () -> Int,
) {
    private val queries = Sql(dataSource)

    private data class Visitor(
        val playerId: Int,
        val guildId: Int,
    )

    private val lastVisits = ConcurrentHashMap<Visitor, Int>()

    init {
        dataSource
            .readInto("recent forum visits", ArrayList(), SELECT_RECENT, { int(clock() - RECENT_SECONDS) }) {
                Visitor(it.getInt("player_id"), it.getInt("guild_id")) to it.getInt("visited_at")
            }.forEach { (visitor, at) -> lastVisits[visitor] = at }
    }

    fun hasRecentVisit(
        userId: Int,
        guildId: Int,
    ): Boolean = lastVisits[Visitor(userId, guildId)]?.let(::isRecent) == true

    fun add(
        userId: Int,
        guildId: Int,
    ) {
        val now = clock()
        lastVisits[Visitor(userId, guildId)] = now
        guildStorageLog.orFallback("guild storage: forum visit of player $userId to guild $guildId", Unit) {
            queries.write(UPSERT_VISIT) {
                int(userId)
                int(guildId)
                int(now)
            }
        }
    }

    fun groupsByRecentVisits(): List<Int> {
        lastVisits.entries.removeIf { !isRecent(it.value) }
        return lastVisits.entries
            .filter { isRecent(it.value) }
            .groupingBy { it.key.guildId }
            .eachCount()
            .entries
            .sortedByDescending { it.value }
            .map { it.key }
    }

    private fun isRecent(visitedAt: Int) = visitedAt > clock() - RECENT_SECONDS

    private companion object {
        const val RECENT_SECONDS = 7 * 24 * 60 * 60
        const val SELECT_RECENT = "SELECT player_id, guild_id, visited_at FROM guild_forum_visits WHERE visited_at > ?"
        const val UPSERT_VISIT =
            "INSERT INTO guild_forum_visits (player_id, guild_id, visited_at) VALUES (?, ?, ?)" +
                " ON DUPLICATE KEY UPDATE visited_at = VALUES(visited_at)"
    }
}
