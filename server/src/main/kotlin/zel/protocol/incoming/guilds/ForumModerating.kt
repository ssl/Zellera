package zel.protocol.incoming.guilds

import zel.game.guilds.ForumRules
import zel.game.guilds.Guild
import zel.game.guilds.forums.ForumModeration
import zel.game.guilds.forums.ForumState
import zel.game.guilds.forums.ForumThread
import zel.protocol.generated.UpdateForumSettingsIn
import zel.protocol.outgoing.generic.alerts.BubbleAlert
import zel.protocol.typed.PacketIds

internal class ForumModerating(
    private val services: GuildServices,
    private val answers: ForumAnswers,
) {
    private val guilds = services.guilds
    private val threads = guilds.forumThreads
    private val presenter = guilds.forumPresenter
    private val messages = services.forumMessages

    fun pinOrLock(
        caller: Caller,
        guildId: Int,
        threadId: Int,
        pinned: Boolean,
        locked: Boolean,
    ) {
        if (!PacketIds.isId(guildId) || !PacketIds.isId(threadId)) {
            return
        }
        val (guild, thread) = answers.threadToChange(caller, guildId, threadId) ?: return
        val access = answers.access(caller, guild)
        if (!access.isKnownToGroup || !access.mayPinOrLock) {
            answers.outdated(caller, guild)
            return
        }
        val pinChanged = pinned != thread.isPinned
        if (pinChanged) {
            answers.bubble(caller, if (pinned) BubbleAlert.THREAD_PINNED else BubbleAlert.THREAD_UNPINNED)
        }
        if (locked != thread.isLocked) {
            answers.bubble(caller, if (locked) BubbleAlert.THREAD_LOCKED else BubbleAlert.THREAD_UNLOCKED)
        }
        thread.setPinnedAndLocked(pinned, locked)
        threads.save(thread)
        messages.sendUpdatedThread(caller.client, thread.guildId, presenter.thread(thread, access.maySeeHidden))
        if (pinChanged) sendFirstPage(caller, guild)
    }

    fun moderateThread(
        caller: Caller,
        guildId: Int,
        threadId: Int,
        state: Int,
    ) {
        if (!PacketIds.isId(guildId) || !PacketIds.isId(threadId) ||
            !GuildForumInput.isModerationState(state)
        ) {
            return
        }
        val (guild, thread) = answers.threadToChange(caller, guildId, threadId) ?: return
        val access = answers.access(caller, guild)
        val target = ForumState.fromCode(state)
        if (!access.isKnownToGroup || !access.mayModerate(thread.state, target)) {
            answers.outdated(caller, guild)
            return
        }
        if (target == ForumState.HIDDEN_BY_STAFF) {
            deleteThread(caller, guild, thread)
            return
        }
        thread.moderate(target, ForumModeration(caller.userId, services.unixTime()))
        threads.save(thread)
        messages.sendUpdatedThread(caller.client, thread.guildId, presenter.thread(thread, access.maySeeHidden))
        when (target) {
            ForumState.HIDDEN_BY_GROUP_ADMIN -> {
                answers.bubble(caller, BubbleAlert.THREAD_HIDDEN)
            }

            ForumState.RESTORED -> {
                answers.bubble(caller, BubbleAlert.THREAD_RESTORED)
            }

            ForumState.OPEN, ForumState.HIDDEN_BY_STAFF -> {}
        }
    }

    private fun deleteThread(
        caller: Caller,
        guild: Guild,
        thread: ForumThread,
    ) {
        threads.delete(thread)
        guilds.forumMarks.invalidateUnread(guild.id)
        answers.bubble(caller, BubbleAlert.THREAD_HIDDEN)
        sendFirstPage(caller, guild)
    }

    fun moderatePost(
        caller: Caller,
        guildId: Int,
        threadId: Int,
        postId: Int,
        state: Int,
    ) {
        val validIds = listOf(guildId, threadId, postId).all(PacketIds::isId)
        if (!validIds || !GuildForumInput.isModerationState(state)) {
            return
        }
        val (guild, thread) = answers.threadToChange(caller, guildId, threadId) ?: return
        val post = thread.post(postId)
        val access = answers.access(caller, guild)
        val target = ForumState.fromCode(state)
        if (post == null || !access.isKnownToGroup || !access.mayModerate(post.state, target)) {
            answers.outdated(caller, guild)
            return
        }
        post.moderate(target, ForumModeration(caller.userId, services.unixTime()))
        messages.sendUpdatedMessage(caller.client, guild.id, thread.id, presenter.post(post, true))
        services.threading.run { threads.save(post) }
        when (target) {
            ForumState.HIDDEN_BY_GROUP_ADMIN, ForumState.HIDDEN_BY_STAFF -> {
                answers.bubble(caller, BubbleAlert.POST_HIDDEN)
            }

            ForumState.RESTORED -> {
                answers.bubble(caller, BubbleAlert.POST_RESTORED)
            }

            ForumState.OPEN -> {}
        }
    }

    fun settings(
        caller: Caller,
        request: UpdateForumSettingsIn,
    ) {
        val rules = requestedRules(request)
        if (!PacketIds.isId(request.guildId) || rules == null) {
            return
        }
        val guild = guilds.byId(request.guildId) ?: return
        if (!answers.access(caller, guild).isOwner) {
            answers.outdated(caller, guild)
            return
        }
        guild.ruleForum(rules)
        services.threading.run { guilds.save(guild) }
        answers.bubble(caller, BubbleAlert.FORUM_SETTINGS_SAVED)
        answers.sendForumData(caller, guild)
    }

    /** Null when a level is not offered for its setting. */
    private fun requestedRules(request: UpdateForumSettingsIn): ForumRules? =
        ForumRules.fromCodes(request.canRead, request.postMessages, request.postThreads, request.modForum)

    private fun sendFirstPage(
        caller: Caller,
        guild: Guild,
    ) {
        val page = ForumReading.pageOfThreads(ForumReading.pinnedFirst(threads.threadList(guild.id)), 0)
        messages.sendThreads(caller.client, guild.id, 0, page.map { presenter.thread(it, true) })
    }
}
