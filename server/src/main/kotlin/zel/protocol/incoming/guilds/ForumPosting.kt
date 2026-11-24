package zel.protocol.incoming.guilds

import zel.game.guilds.Guild
import zel.protocol.generated.RoomMessageNotificationOut
import zel.protocol.generated.RoomMessageNotificationOutCodec
import zel.protocol.typed.PacketIds

internal class ForumPosting(
    private val services: GuildServices,
    private val answers: ForumAnswers,
) {
    private val guilds = services.guilds
    private val threads = guilds.forumThreads
    private val presenter = guilds.forumPresenter
    private val messages = services.forumMessages
    private val rate = GuildForumPostRate()

    fun post(
        caller: Caller,
        guildId: Int,
        threadId: Int,
        rawSubject: String,
        rawMessage: String,
    ) {
        if (!PacketIds.isId(guildId) || threadId < 0) return
        val opensThread = threadId == 0
        val typedSubject = GuildForumInput.normalize(rawSubject)
        val typedMessage = GuildForumInput.normalize(rawMessage)
        val wellFormed =
            GuildForumInput.isMessage(typedMessage) && (!opensThread || GuildForumInput.isSubject(typedSubject))
        if (!wellFormed || !rate.mayPost(caller.userId, System.currentTimeMillis())) return
        val guild = guilds.byId(guildId) ?: return
        val message = services.wordFilter.filter(typedMessage, caller.player)
        if (opensThread) {
            open(caller, guild, services.wordFilter.filter(typedSubject, caller.player), message)
        } else {
            reply(caller, guild, threadId, message)
        }
    }

    private fun open(
        caller: Caller,
        guild: Guild,
        subject: String,
        message: String,
    ) {
        if (!answers.access(caller, guild).mayStartThread) {
            answers.outdated(caller, guild)
            return
        }
        val thread = threads.open(guild, caller.player, subject, message)
        if (thread == null) {
            answers.failed(caller)
            return
        }
        countAuthor(caller, guild)
        messages.sendNewThread(caller.client, guild.id, presenter.thread(thread, true))
        tellRoomOwner(caller, guild)
    }

    private fun reply(
        caller: Caller,
        guild: Guild,
        threadId: Int,
        message: String,
    ) {
        val thread = threads.thread(threadId)?.takeIf { it.guildId == guild.id }
        val access = answers.access(caller, guild)
        if (thread == null || (thread.isLocked && !access.isStaff) || !access.mayPostMessage) {
            answers.outdated(caller, guild)
            return
        }
        val post = threads.reply(thread, caller.player, message)
        if (post == null) {
            answers.failed(caller)
            return
        }
        countAuthor(caller, guild)
        messages.sendNewMessage(caller.client, guild.id, post.threadId, presenter.post(post, true))
        tellRoomOwner(caller, guild)
    }

    private fun countAuthor(
        caller: Caller,
        guild: Guild,
    ) {
        rate.posted(caller.userId, System.currentTimeMillis())
        caller.player.stats.tally.forumPosts += 1
        guilds.forumMarks.invalidateUnread(guild.id)
    }

    private fun tellRoomOwner(
        caller: Caller,
        guild: Guild,
    ) {
        if (guild.roomId <= 0) return
        val room = services.rooms.room(guild.roomId)
        if (room == null || room.ownerId == caller.userId) return
        services.players
            .find(room.ownerId)
            ?.client
            ?.send(RoomMessageNotificationOutCodec, RoomMessageNotificationOut(room.id, room.name, 1))
    }
}
