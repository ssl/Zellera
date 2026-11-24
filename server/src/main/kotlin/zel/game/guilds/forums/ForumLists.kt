package zel.game.guilds.forums

import zel.db.Params
import zel.db.Sql
import zel.db.orFallback
import zel.game.guilds.ForumLevel
import zel.game.guilds.Guild
import zel.game.guilds.MemberLevel
import zel.game.guilds.guildStorageLog
import zel.util.SECONDS_PER_WEEK
import javax.sql.DataSource

class ForumLists(
    dataSource: DataSource,
    private val clock: () -> Int,
    private val guildById: (Int) -> Guild?,
) {
    private val queries = Sql(dataSource)

    /** Not [complete] when the database could not be read. */
    data class Found(
        val guilds: Set<Guild>,
        val complete: Boolean,
    )

    fun mostActive(): Found =
        found(MOST_ACTIVE, { int(clock() - SECONDS_PER_WEEK) }) { it.forumRules.read == ForumLevel.EVERYONE }

    fun ofMember(userId: Int): Found =
        found(
            OF_MEMBER,
            {
                int(userId)
                int(MemberLevel.REGULAR.code)
            },
        ) { true }

    private fun found(
        sql: String,
        bind: Params.() -> Unit,
        keep: (Guild) -> Boolean,
    ): Found {
        val ids =
            guildStorageLog.orFallback("guild storage: list forums", null) {
                queries.select(sql, bind).rows { rows -> rows.getInt(1) }
            } ?: return Found(emptySet(), complete = false)
        val guilds = ids.mapNotNull(guildById).filter { it.hasForum && keep(it) }
        return Found(guilds.toCollection(LinkedHashSet()), complete = true)
    }

    private companion object {
        const val MOST_ACTIVE =
            "SELECT t.guild_id FROM guild_forum_posts p INNER JOIN guild_forum_threads t ON t.id = p.thread_id " +
                "INNER JOIN guilds g ON g.id = t.guild_id WHERE g.has_forum = 1 AND p.created_at >= ? " +
                "GROUP BY t.guild_id ORDER BY COUNT(*) DESC, t.guild_id ASC LIMIT 100"
        const val OF_MEMBER =
            "SELECT m.guild_id FROM guild_members m INNER JOIN guilds g ON g.id = m.guild_id " +
                "WHERE m.player_id = ? AND m.level <= ? AND g.has_forum = 1 ORDER BY m.member_since ASC"
    }
}
