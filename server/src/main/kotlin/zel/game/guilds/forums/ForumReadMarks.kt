package zel.game.guilds.forums

import zel.db.Params
import zel.db.Sql
import zel.db.orFallback
import zel.db.write
import zel.game.guilds.guildStorageLog
import java.util.concurrent.ConcurrentHashMap
import javax.sql.DataSource

class ForumReadMarks(
    private val dataSource: DataSource,
    private val millis: () -> Long = System::currentTimeMillis,
) {
    private val queries = Sql(dataSource)

    private data class Stamped(
        val value: Int,
        val cachedAt: Long,
    )

    private data class Reader(
        val userId: Int,
        val guildId: Int,
    )

    private data class UnreadSince(
        val guildId: Int,
        val since: Int,
    )

    private val lastSeen = ConcurrentHashMap<Reader, Stamped>()
    private val unread = ConcurrentHashMap<UnreadSince, Stamped>()

    fun unreadPosts(
        userId: Int,
        guildId: Int,
    ): Int {
        val since = cached(lastSeen, Reader(userId, guildId), LAST_SEEN_TTL_MS) { lastSeenAt(userId, guildId) }
        return cached(unread, UnreadSince(guildId, since), UNREAD_TTL_MS) { newPostCount(guildId, since) }
    }

    fun unreadForums(userId: Int): Int = countOf(UNREAD_FORUMS) { int(userId) }

    fun markRead(
        userId: Int,
        guildId: Int,
        now: Int,
    ) {
        guildStorageLog.orFallback("guild storage: mark read", Unit) {
            dataSource.connection.use { connection ->
                connection.write(MARK_READ) {
                    int(userId)
                    int(guildId)
                    int(now)
                    int(now)
                }
            }
        }
        lastSeen.remove(Reader(userId, guildId))
        invalidateUnread(guildId)
    }

    fun invalidateUnread(guildId: Int) {
        unread.keys.removeIf { it.guildId == guildId }
    }

    private fun <K : Any> cached(
        cache: MutableMap<K, Stamped>,
        key: K,
        ttlMs: Long,
        read: () -> Int,
    ): Int {
        val now = millis()
        cache[key]?.takeIf { now - it.cachedAt < ttlMs }?.let { return it.value }
        return read().also { cache[key] = Stamped(it, now) }
    }

    private fun lastSeenAt(
        userId: Int,
        guildId: Int,
    ): Int =
        countOf(LAST_SEEN) {
            int(userId)
            int(guildId)
        }

    private fun newPostCount(
        guildId: Int,
        since: Int,
    ): Int =
        countOf(NEW_POSTS) {
            int(guildId)
            int(since)
        }

    private fun countOf(
        sql: String,
        bind: Params.() -> Unit,
    ): Int =
        guildStorageLog.orFallback("guild storage: count forum posts", 0) {
            queries.select(sql, bind).first { rows -> rows.getInt(1) } ?: 0
        }

    private companion object {
        const val LAST_SEEN_TTL_MS = 5 * 60 * 1000L
        const val UNREAD_TTL_MS = 2 * 60 * 1000L

        const val LAST_SEEN =
            "SELECT `read_at` FROM `guild_forum_reads` WHERE `player_id` = ? AND `guild_id` = ? LIMIT 1"
        const val NEW_POSTS =
            "SELECT COUNT(*) FROM `guild_forum_posts` " +
                "JOIN `guild_forum_threads` ON `guild_forum_threads`.`id` = `guild_forum_posts`.`thread_id` " +
                "WHERE `guild_forum_threads`.`guild_id` = ? AND `guild_forum_posts`.`created_at` > ?"

        const val UNREAD_FORUMS =
            "SELECT COUNT(*) FROM `guilds` g" +
                " JOIN `guild_members` m ON m.`guild_id` = g.`id` AND m.`player_id` = ?" +
                " LEFT JOIN `guild_forum_reads` r ON r.`guild_id` = g.`id` AND r.`player_id` = m.`player_id`" +
                " WHERE g.`has_forum` = TRUE AND EXISTS (" +
                "SELECT 1 FROM `guild_forum_posts` c JOIN `guild_forum_threads` t ON t.`id` = c.`thread_id`" +
                " WHERE t.`guild_id` = g.`id` AND c.`created_at` > COALESCE(r.`read_at`, 0))"
        const val MARK_READ =
            "INSERT INTO `guild_forum_reads` (`player_id`, `guild_id`, `read_at`) VALUES (?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE `read_at` = ?"
    }
}
