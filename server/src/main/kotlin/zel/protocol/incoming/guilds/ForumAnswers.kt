package zel.protocol.incoming.guilds

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import zel.game.guilds.Guild
import zel.game.guilds.forums.ForumAccess
import zel.game.guilds.forums.ForumThread
import zel.game.permissions.isAnyGroupOwner
import zel.protocol.outgoing.generic.alerts.BubbleAlert
import zel.protocol.outgoing.generic.alerts.BubbleAlertMessage
import zel.protocol.outgoing.handshake.ErrorReport

internal class ForumAnswers(
    private val services: GuildServices,
) {
    private val guilds = services.guilds
    private val messages = services.forumMessages

    fun access(
        caller: Caller,
        guild: Guild,
    ) = ForumAccess(
        guild,
        caller.userId,
        guilds.members.find(guild.id, caller.userId),
        caller.player.isAnyGroupOwner,
    )

    /** The client asked for what it could no longer know to be allowed: it gets the forum as it is now. */
    fun outdated(
        caller: Caller,
        guild: Guild,
    ) = sendForumData(caller, guild)

    /** The request was fine and the server could not do it. */
    fun failed(caller: Caller) = caller.client.send(ErrorReport.forRequest(NO_REQUEST))

    fun bubble(
        caller: Caller,
        alert: BubbleAlert,
    ) = caller.client.send(BubbleAlertMessage(alert.key).compose())

    @Suppress("TooGenericExceptionCaught") // answered with the forum error code
    fun sendForumData(
        caller: Caller,
        guild: Guild,
    ) {
        try {
            val access = access(caller, guild)
            if (access.mayRead) {
                messages.sendForumData(
                    caller.client,
                    guilds.forumSummaries.of(guild, caller.player),
                    access.permissions(),
                )
            } else {
                messages.sendUnreadableForumData(caller.client, guild.id, access.readRefusal, services.unixTime())
            }
        } catch (e: RuntimeException) {
            log.error("Caught exception", e)
            failed(caller)
        }
    }

    /** The thread of that forum; an unknown forum gets no answer, a thread that is not there the forum as it is. */
    fun threadToChange(
        caller: Caller,
        guildId: Int,
        threadId: Int,
    ): Pair<Guild, ForumThread>? {
        val guild = guilds.byId(guildId) ?: return null
        val thread = guilds.forumThreads.thread(threadId)?.takeIf { it.guildId == guildId }
        if (thread == null) outdated(caller, guild)
        return thread?.let { guild to it }
    }

    private companion object {
        val log: Logger = LoggerFactory.getLogger(ForumAnswers::class.java)
        const val NO_REQUEST = 0
    }
}
