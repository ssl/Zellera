package zel.protocol.incoming.guilds

import zel.protocol.generated.GetForumStatsInCodec
import zel.protocol.generated.GetForumsListInCodec
import zel.protocol.generated.GetMessagesInCodec
import zel.protocol.generated.GetThreadInCodec
import zel.protocol.generated.GetThreadsInCodec
import zel.protocol.generated.GetUnreadForumsCountInCodec
import zel.protocol.generated.ModerateMessageInCodec
import zel.protocol.generated.ModerateThreadInCodec
import zel.protocol.generated.PostMessageInCodec
import zel.protocol.generated.UpdateForumReadMarkerInCodec
import zel.protocol.generated.UpdateForumSettingsInCodec
import zel.protocol.generated.UpdateThreadInCodec
import zel.protocol.typed.PacketPackage
import zel.protocol.typed.PacketRoute

class ForumPackets(
    services: GuildServices,
) : PacketPackage {
    private val answers = ForumAnswers(services)
    private val reading = ForumReading(services, answers)
    private val posting = ForumPosting(services, answers)
    private val moderating = ForumModerating(services, answers)

    override val name = "guild-forums"

    override val routes: List<PacketRoute<*>> =
        listOf(
            callerRoute(GetUnreadForumsCountInCodec, UNREAD_GAP) { caller, _ -> reading.unreadCount(caller) },
            callerRoute(GetForumStatsInCodec, GAP) { caller, m -> reading.forumStats(caller, m.guildId) },
            callerRoute(GetForumsListInCodec, GAP) { caller, m -> reading.forumList(caller, m.mode, m.offset) },
            callerRoute(GetThreadsInCodec, GAP) { caller, m -> reading.threadPage(caller, m.guildId, m.index) },
            callerRoute(GetThreadInCodec, GAP) { caller, m -> reading.thread(caller, m.guildId, m.threadId) },
            callerRoute(GetMessagesInCodec, GAP) { caller, m ->
                reading.posts(caller, m.guildId, m.threadId, m.index, m.limit)
            },
            callerRoute(PostMessageInCodec, SLOW_GAP) { caller, m ->
                posting.post(caller, m.guildId, m.threadId, m.subject, m.message)
            },
            callerRoute(UpdateThreadInCodec, SLOW_GAP) { caller, m ->
                moderating.pinOrLock(caller, m.guildId, m.threadId, m.isPinned, m.isLocked)
            },
            callerRoute(ModerateThreadInCodec, SLOW_GAP) { caller, m ->
                moderating.moderateThread(caller, m.guildId, m.threadId, m.state)
            },
            callerRoute(ModerateMessageInCodec, SLOW_GAP) { caller, m ->
                moderating.moderatePost(caller, m.guildId, m.threadId, m.messageId, m.state)
            },
            callerRoute(UpdateForumSettingsInCodec, SLOW_GAP) { caller, m -> moderating.settings(caller, m) },
            callerRoute(UpdateForumReadMarkerInCodec, SLOW_GAP) { caller, m -> reading.markRead(caller, m) },
        )

    private companion object {
        const val GAP = 500L
        const val SLOW_GAP = 2000L
        const val UNREAD_GAP = 5000L
    }
}
