package zel.protocol.incoming.moderation

import zel.game.guilds.forums.ForumThreadStore
import zel.game.moderation.ChatlogLine
import zel.game.moderation.ModerationService
import zel.game.moderation.SupportTicket
import zel.game.moderation.TicketChatlogType
import zel.game.moderation.TicketType
import zel.game.players.Players
import zel.protocol.generated.CfhChatlogOutCodec
import zel.protocol.outgoing.moderation.ModerationChatlogs
import zel.protocol.typed.PacketIds
import zel.protocol.typed.PacketSession
import zel.room.listing.RoomListings

class IssueChatlogs(
    private val manager: ModerationService,
    private val rooms: RoomListings,
    private val players: Players,
    private val forumThreads: ForumThreadStore,
) {
    private class ChatlogSource(
        val type: TicketChatlogType,
        val lines: List<ChatlogLine>,
    )

    fun send(
        s: PacketSession,
        ticketId: Int,
    ) {
        val issue = manager.tickets.byId(ticketId)?.takeIf { PacketIds.isId(ticketId) }
        val room = issue?.let { rooms.room(it.roomId) }
        val source = issue?.let { chatlogSource(it) }
        val message =
            if (issue != null && source != null) {
                val picked = source.type == TicketChatlogType.ROOM_TALK || source.type == TicketChatlogType.PRIVATE_TALK
                val marked = if (picked) issue.markPickedIn(source.lines) else source.lines
                val lines = marked.sortedBy(ChatlogLine::timestamp)
                ModerationChatlogs.ticketLog(issue, lines, room?.name.orEmpty(), source.type, room?.guildId ?: 0)
            } else {
                null
            }
        if (message != null) s.send(CfhChatlogOutCodec, message)
    }

    private fun chatlogSource(issue: SupportTicket): ChatlogSource =
        when (issue.type) {
            TicketType.MESSENGER -> {
                ChatlogSource(
                    TicketChatlogType.PRIVATE_TALK,
                    manager.chats.conversation(issue.accusedId, issue.reporterId),
                )
            }

            TicketType.FORUM -> {
                forumChatlog(issue)
            }

            TicketType.GUIDE -> {
                ChatlogSource(TicketChatlogType.PRIVATE_TALK, issue.pickedLines.orEmpty())
            }

            TicketType.PHOTO -> {
                if (issue.photo != null) {
                    ChatlogSource(TicketChatlogType.ROOM_PICTURE, manager.chats.roomLines(issue.roomId))
                } else {
                    ChatlogSource(TicketChatlogType.ROOM_TALK, emptyList())
                }
            }

            else -> {
                ChatlogSource(
                    TicketChatlogType.ROOM_TALK,
                    if (issue.roomId > 0) {
                        manager.chats.roomLines(issue.roomId)
                    } else {
                        manager.chats.linesBy(issue.accusedId) + manager.chats.linesBy(issue.reporterId)
                    },
                )
            }
        }

    private fun forumChatlog(issue: SupportTicket): ChatlogSource {
        val comment = issue.forumPostId != -1
        val type = if (comment) TicketChatlogType.FORUM_REPLY else TicketChatlogType.FORUM_POST
        val lines =
            forumThreads.thread(issue.forumThreadId)?.posts().orEmpty().map {
                ChatlogLine(
                    it.createdAt,
                    it.authorId,
                    players.profileOf(it.authorId)?.name.orEmpty(),
                    it.message,
                    comment && it.id == issue.forumPostId,
                )
            }
        return ChatlogSource(type, lines)
    }
}
