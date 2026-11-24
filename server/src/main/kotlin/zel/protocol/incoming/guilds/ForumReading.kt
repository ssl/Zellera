@file:Suppress("MagicNumber") // wire codes of the forum views

package zel.protocol.incoming.guilds

import zel.game.guilds.Guild
import zel.game.guilds.forums.ForumLists
import zel.game.guilds.forums.ForumThread
import zel.protocol.generated.UpdateForumReadMarkerIn
import zel.protocol.outgoing.generic.alerts.BubbleAlert
import zel.protocol.outgoing.handshake.ErrorReport
import zel.protocol.typed.PacketIds
import org.slf4j.LoggerFactory

internal class ForumReading(
    private val services: GuildServices,
    private val answers: ForumAnswers,
) {
    private val guilds = services.guilds
    private val threads = guilds.forumThreads
    private val presenter = guilds.forumPresenter
    private val messages = services.forumMessages

    fun unreadCount(caller: Caller) =
        messages.sendUnreadCount(caller.client, guilds.forumMarks.unreadForums(caller.userId))

    fun forumStats(
        caller: Caller,
        guildId: Int,
    ) {
        val guild = forumOrErrorReport(caller, guildId, ForumRequest.STATS) ?: return
        answers.sendForumData(caller, guild)
        if (!answers.access(caller, guild).mayRead) return
        if (!guilds.forumVisits.hasRecentVisit(caller.userId, guildId)) guilds.forumVisits.add(caller.userId, guildId)
    }

    fun forumList(
        caller: Caller,
        mode: Int,
        offset: Int,
    ) {
        if (!GuildForumInput.isListOffset(offset)) {
            return
        }
        val found =
            when (mode) {
                MODE_MOST_ACTIVE -> guilds.forumLists.mostActive().orErrorTo(caller)
                MODE_MOST_VIEWED -> guilds.mostViewedForums()
                MODE_MY_FORUMS -> guilds.forumLists.ofMember(caller.userId).orErrorTo(caller)
                else -> return
            }
        val page = found.drop(offset).take(FORUMS_PER_PAGE).map { guilds.forumSummaries.of(it, caller.player) }
        messages.sendForumList(caller.client, mode, found.size, offset, page)
    }

    private fun ForumLists.Found.orErrorTo(caller: Caller): Set<Guild> {
        if (!complete) answers.failed(caller)
        return guilds
    }

    @Suppress("TooGenericExceptionCaught") // answered with the forum error code
    fun threadPage(
        caller: Caller,
        guildId: Int,
        index: Int,
    ) {
        if (!GuildForumInput.isListOffset(index)) {
            return
        }
        val guild = forumOrErrorReport(caller, guildId, ForumRequest.THREADS) ?: return
        val access = answers.access(caller, guild)
        if (!access.mayRead) {
            answers.sendForumData(caller, guild)
            return
        }
        val sorted =
            try {
                pinnedFirst(threads.threadList(guild.id))
            } catch (e: RuntimeException) {
                log.error("Caught exception", e)
                answers.failed(caller)
                return
            }
        val page = pageOfThreads(sorted, index).map { presenter.thread(it, access.maySeeHidden) }
        messages.sendThreads(caller.client, guild.id, index, page)
    }

    fun thread(
        caller: Caller,
        guildId: Int,
        threadId: Int,
    ) {
        val guild = forumOrErrorReport(caller, guildId, ForumRequest.THREAD) ?: return
        val access = answers.access(caller, guild)
        if (!access.mayRead) {
            answers.sendForumData(caller, guild)
            return
        }
        val thread = threadOrErrorReport(caller, guildId, threadId, ForumRequest.THREAD) ?: return
        messages.sendUpdatedThread(caller.client, thread.guildId, presenter.thread(thread, access.maySeeHidden))
    }

    fun posts(
        caller: Caller,
        guildId: Int,
        threadId: Int,
        index: Int,
        limit: Int,
    ) {
        if (!GuildForumInput.isPage(index, limit)) {
            return
        }
        val guild = forumOrErrorReport(caller, guildId, ForumRequest.MESSAGES) ?: return
        val access = answers.access(caller, guild)
        if (!access.mayRead) {
            answers.sendForumData(caller, guild)
            return
        }
        val thread = threadOrErrorReport(caller, guildId, threadId, ForumRequest.MESSAGES) ?: return
        if (thread.state.isHidden && !access.maySeeHidden) {
            answers.bubble(caller, BubbleAlert.FORUM_CLOSED_TO_CALLER)
            answers.sendForumData(caller, guild)
            return
        }
        val page = thread.posts(limit, index).map { presenter.post(it, access.maySeeHidden) }
        messages.sendMessages(caller.client, guildId, threadId, index, page)
    }

    fun markRead(
        caller: Caller,
        request: UpdateForumReadMarkerIn,
    ) {
        if (!GuildForumInput.isReadBatch(request.marks.size)) return
        request.marks
            .filter { PacketIds.isId(it.guildId) }
            .forEach { mark ->
                val guild = guilds.byId(mark.guildId)
                if (guild != null && guild.hasForum && answers.access(caller, guild).mayRead) {
                    guilds.forumMarks.markRead(caller.userId, mark.guildId, services.unixTime())
                }
            }
    }

    private fun forumOrErrorReport(
        caller: Caller,
        guildId: Int,
        request: ForumRequest,
    ): Guild? {
        val guild = guilds.byValidId(guildId)?.takeIf { it.hasForum }
        if (guild == null) caller.client.send(ErrorReport.forRequest(request.errorId))
        return guild
    }

    private fun threadOrErrorReport(
        caller: Caller,
        guildId: Int,
        threadId: Int,
        request: ForumRequest,
    ): ForumThread? {
        val thread =
            threadId
                .takeIf(PacketIds::isId)
                ?.let(threads::thread)
                ?.takeIf { it.guildId == guildId }
        if (thread == null) caller.client.send(ErrorReport.forRequest(request.errorId))
        return thread
    }

    private enum class ForumRequest(
        val errorId: Int,
    ) {
        STATS(1600),
        THREADS(1601),
        MESSAGES(1602),
        THREAD(1604),
    }

    companion object {
        private const val MODE_MOST_ACTIVE = 0
        private const val MODE_MOST_VIEWED = 1
        private const val MODE_MY_FORUMS = 2
        private const val FORUMS_PER_PAGE = 20
        private const val THREADS_PER_PAGE = 20
        private val log = LoggerFactory.getLogger(ForumReading::class.java)

        fun pinnedFirst(all: List<ForumThread>): List<ForumThread> =
            all.sortedBy { if (it.isPinned) Int.MAX_VALUE else it.updatedAt }.reversed()

        fun <T> pageOfThreads(
            sorted: List<T>,
            index: Int,
        ): List<T> = sorted.drop(index.coerceAtLeast(0)).take(THREADS_PER_PAGE)
    }
}
