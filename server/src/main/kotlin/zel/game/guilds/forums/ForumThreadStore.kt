package zel.game.guilds.forums

import zel.game.guilds.Guild
import zel.game.players.Player
import java.util.concurrent.ConcurrentHashMap

class ForumThreadStore(
    private val tables: ForumTables,
    private val clock: () -> Int,
) {
    private val threadsByGuild = ConcurrentHashMap<Int, MutableSet<ForumThread>>()
    private val threadsById = ConcurrentHashMap<Int, ForumThread>()
    private val postsById = ConcurrentHashMap<Int, ForumPost>()

    fun thread(id: Int): ForumThread? = threadsById[id] ?: tables.thread(id)?.let(::remember)

    fun postIn(
        thread: ForumThread,
        postId: Int,
    ): ForumPost? = thread.post(postId)

    fun threadList(guildId: Int): List<ForumThread> {
        val listed = threadsByGuild.computeIfAbsent(guildId) { tables.threadsOf(it).mapTo(HashSet(), ::remember) }
        return synchronized(listed) { listed.toList() }
    }

    fun open(
        guild: Guild,
        opener: Player,
        subject: String,
        message: String,
    ): ForumThread? {
        val openerId = opener.profile.id
        val now = clock()
        val (id, firstId) = tables.openThread(guild.id, openerId, subject, message, now) ?: return null
        val thread = hold(ForumThread(id, guild.id, openerId, subject, now, ::postsOf))
        thread.posted(remembered(ForumPost(firstId, id, openerId, message, now)), now)
        return thread
    }

    fun reply(
        thread: ForumThread,
        author: Player,
        message: String,
    ): ForumPost? {
        val authorId = author.profile.id
        val now = clock()
        val id = tables.addPost(thread.id, authorId, message, now) ?: return null
        return remembered(ForumPost(id, thread.id, authorId, message, now)).also { thread.posted(it, now) }
    }

    fun save(thread: ForumThread) {
        if (thread.hasUnsavedChanges && tables.update(thread)) thread.saved()
    }

    fun save(post: ForumPost) {
        if (post.hasUnsavedChanges && tables.update(post)) post.saved()
    }

    fun delete(thread: ForumThread) {
        tables.deleteThread(thread.id)
        forget(thread.guildId)
    }

    fun forget(guildId: Int) {
        val dropped = threadsByGuild.remove(guildId) ?: return
        synchronized(dropped) { dropped.forEach { threadsById.remove(it.id) } }
    }

    fun flush() {
        threadsByGuild.values.toList().forEach { set -> synchronized(set) { set.toList() }.forEach(::save) }
        threadsById.clear()
        threadsByGuild.clear()
        postsById.clear()
    }

    private fun remember(row: ForumTables.ThreadRow): ForumThread =
        with(row) {
            ForumThread(
                id,
                guildId,
                openerId,
                subject,
                createdAt,
                ::postsOf,
                postCount,
                updatedAt,
                flags,
                lastPost?.let(::remembered),
            )
        }.let(::hold)

    private fun postsOf(threadId: Int): List<ForumPost> = tables.postsOf(threadId).map(::remembered)

    /** One object per post id, so a moderated newest post shows as such in the thread list. */
    private fun remembered(post: ForumPost): ForumPost = postsById.putIfAbsent(post.id, post) ?: post

    /** Keeps one object per thread id; a thread joins its guild's list only when that list is loaded. */
    private fun hold(thread: ForumThread): ForumThread {
        val kept = threadsById.putIfAbsent(thread.id, thread) ?: thread
        threadsByGuild[kept.guildId]?.let { listed -> synchronized(listed) { listed.add(kept) } }
        return kept
    }
}
