package zel.game.guilds.forums

import zel.db.allOrNothing
import zel.db.insertForId
import zel.db.orFallback
import zel.db.write
import zel.game.guilds.guildStorageLog
import zel.game.guilds.readInto
import java.sql.Connection
import java.sql.ResultSet
import javax.sql.DataSource

class ForumTables(
    private val dataSource: DataSource,
) {
    data class ThreadRow(
        val id: Int,
        val guildId: Int,
        val openerId: Int,
        val subject: String,
        val postCount: Int,
        val createdAt: Int,
        val updatedAt: Int,
        val flags: ForumThread.Flags,
        val lastPost: ForumPost?,
    )

    fun thread(id: Int): ThreadRow? =
        dataSource
            .readInto("forum thread $id", ArrayList(), "$SELECT_THREADS WHERE t.id = ?", { int(id) }, ::threadRow)
            .firstOrNull()

    fun threadsOf(guildId: Int): List<ThreadRow> =
        dataSource.readInto(
            "forum threads of guild $guildId",
            ArrayList(),
            "$SELECT_THREADS WHERE t.guild_id = ?",
            { int(guildId) },
            ::threadRow,
        )

    fun postsOf(threadId: Int): List<ForumPost> =
        dataSource.readInto(
            "posts of forum thread $threadId",
            ArrayList(),
            SELECT_POSTS_OF_THREAD,
            { int(threadId) },
            { postRow(it, "") },
        )

    /** A thread and its first post, both or neither: the ids of the two. */
    fun openThread(
        guildId: Int,
        openerId: Int,
        subject: String,
        message: String,
        now: Int,
    ): Pair<Int, Int>? =
        guildStorageLog.orFallback("guild storage: open thread", null) {
            dataSource.connection.use { connection ->
                connection.allOrNothing {
                    val threadId =
                        connection.insertForId(INSERT_THREAD) {
                            int(guildId)
                            int(openerId)
                            string(subject)
                            int(now)
                            int(now)
                        } ?: return@allOrNothing null
                    connection.addPost(threadId, openerId, message, now)?.let { threadId to it }
                }
            }
        }

    /** The new post's id; the thread row learns of it in the same commit. */
    fun addPost(
        threadId: Int,
        authorId: Int,
        message: String,
        now: Int,
    ): Int? =
        guildStorageLog.orFallback("guild storage: add post", null) {
            dataSource.connection.use { connection ->
                connection.allOrNothing { connection.addPost(threadId, authorId, message, now) }
            }
        }

    private fun Connection.addPost(
        threadId: Int,
        authorId: Int,
        message: String,
        now: Int,
    ): Int? {
        val postId =
            insertForId(INSERT_POST) {
                int(threadId)
                int(authorId)
                string(message)
                int(now)
            } ?: return null
        write(COUNT_POST) {
            int(postId)
            int(now)
            int(threadId)
        }
        return postId
    }

    fun update(thread: ForumThread): Boolean =
        guildStorageLog.orFallback("guild storage: update thread", false) {
            val flags = thread.flags
            dataSource.connection.use { connection ->
                connection.write(UPDATE_THREAD) {
                    int(flags.state.code)
                    boolean(flags.pinned)
                    boolean(flags.locked)
                    intOrNull(flags.moderation.adminId)
                    intOrNull(flags.moderation.at)
                    int(thread.id)
                }
            }
            true
        }

    fun update(post: ForumPost): Boolean =
        guildStorageLog.orFallback("guild storage: update post", false) {
            val moderation = post.moderation
            dataSource.connection.use { connection ->
                connection.write(UPDATE_POST) {
                    int(post.state.code)
                    intOrNull(moderation.adminId)
                    intOrNull(moderation.at)
                    int(post.id)
                }
            }
            true
        }

    fun deleteThread(id: Int) {
        guildStorageLog.orFallback("guild storage: delete thread", Unit) {
            dataSource.connection.use { connection ->
                connection.write(DELETE_THREAD) { int(id) }
            }
        }
    }

    private fun threadRow(set: ResultSet) =
        ThreadRow(
            set.getInt("id"),
            set.getInt("guild_id"),
            set.getInt("author_id"),
            set.getString("subject").orEmpty(),
            set.getInt("post_count"),
            set.getInt("created_at"),
            set.getInt("updated_at"),
            ForumThread.Flags(
                ForumState.fromCode(set.getInt("state")),
                set.getBoolean("pinned"),
                set.getBoolean("locked"),
                ForumModeration(set.getInt("moderator_id"), set.getInt("moderated_at")),
            ),
            set.getInt("last_id").takeIf { it > 0 }?.let { postRow(set, LAST) },
        )

    private fun postRow(
        set: ResultSet,
        prefix: String,
    ) = ForumPost(
        set.getInt("${prefix}id"),
        set.getInt("${prefix}thread_id"),
        set.getInt("${prefix}author_id"),
        set.getString("${prefix}message").orEmpty(),
        set.getInt("${prefix}created_at"),
        ForumState.fromCode(set.getInt("${prefix}state")),
        ForumModeration(set.getInt("${prefix}moderator_id"), set.getInt("${prefix}moderated_at")),
    )

    private companion object {
        const val LAST = "last_"
        const val POST_COLUMNS = "id, thread_id, author_id, message, created_at, state, moderator_id, moderated_at"
        val LAST_POST_COLUMNS = POST_COLUMNS.split(", ").joinToString(", ") { "p.$it AS $LAST$it" }
        val SELECT_THREADS =
            "SELECT t.id, t.guild_id, t.author_id, t.subject, t.post_count, t.created_at, t.updated_at, t.state, " +
                "t.pinned, t.locked, t.moderator_id, t.moderated_at, $LAST_POST_COLUMNS " +
                "FROM guild_forum_threads t LEFT JOIN guild_forum_posts p ON p.id = t.last_post_id"
        const val SELECT_POSTS_OF_THREAD = "SELECT $POST_COLUMNS FROM guild_forum_posts WHERE thread_id = ? ORDER BY id"
        const val INSERT_THREAD =
            "INSERT INTO guild_forum_threads (guild_id, author_id, subject, created_at, updated_at) " +
                "VALUES (?, ?, ?, ?, ?)"
        const val INSERT_POST =
            "INSERT INTO guild_forum_posts (thread_id, author_id, message, created_at) VALUES (?, ?, ?, ?)"
        const val COUNT_POST =
            "UPDATE guild_forum_threads SET last_post_id = ?, post_count = post_count + 1, updated_at = ? WHERE id = ?"
        const val UPDATE_THREAD =
            "UPDATE guild_forum_threads SET state = ?, pinned = ?, locked = ?, moderator_id = ?, moderated_at = ? " +
                "WHERE id = ?"
        const val UPDATE_POST =
            "UPDATE guild_forum_posts SET state = ?, moderator_id = ?, moderated_at = ? WHERE id = ?"

        /** The posts go with their thread. */
        const val DELETE_THREAD = "DELETE FROM guild_forum_threads WHERE id = ?"
    }
}
