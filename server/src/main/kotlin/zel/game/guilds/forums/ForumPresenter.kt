package zel.game.guilds.forums

import zel.game.players.PlayerProfile

class ForumPresenter(
    private val profile: (Int) -> PlayerProfile?,
    private val clock: () -> Int,
) {
    fun thread(
        thread: ForumThread,
        canSeeHidden: Boolean,
    ): ThreadData {
        val last = thread.lastPost
        val lastAuthor = last?.let { profile(it.authorId) }
        val flags = thread.flags
        val admin = namedAdmin(flags.moderation)
        val now = clock()
        return ThreadData(
            thread.id,
            thread.openerId,
            profile(thread.openerId)?.name.orEmpty(),
            if (!canSeeHidden && flags.state.isHidden) "" else thread.subject,
            flags.pinned,
            flags.locked,
            now - thread.createdAt,
            thread.postCount,
            NO_UNREAD_COUNT,
            last?.id ?: 0,
            lastAuthor?.id ?: 0,
            lastAuthor?.name.orEmpty(),
            now - (last?.createdAt ?: thread.updatedAt),
            flags.state.code.toByte(),
            admin?.id ?: 0,
            admin?.name ?: NO_ADMIN,
            flags.moderation.copy(adminId = admin?.id ?: 0).wireValue(now),
        )
    }

    fun post(
        post: ForumPost,
        canSeeHidden: Boolean,
    ): MessageData {
        val author = profile(post.authorId)
        val moderation = post.moderation
        val admin = namedAdmin(moderation)
        val now = clock()
        return MessageData(
            post.id,
            post.position,
            post.authorId,
            author?.name.orEmpty(),
            author?.figure.orEmpty(),
            now - post.createdAt,
            if (!canSeeHidden && post.state.isHidden) "" else post.message,
            post.state.code.toByte(),
            admin?.id ?: 0,
            admin?.name ?: NO_ADMIN,
            moderation.copy(adminId = admin?.id ?: 0).wireValue(now),
            author?.loadStats()?.tally?.forumPosts ?: 0,
        )
    }

    private fun namedAdmin(moderation: ForumModeration): PlayerProfile? =
        moderation.adminId.takeIf { it > 0 }?.let(profile)

    private companion object {
        const val NO_UNREAD_COUNT = 0

        const val NO_ADMIN = "unknown"
    }
}
