package zel.protocol.outgoing.moderation

import zel.game.moderation.ChatRecordContext
import zel.game.moderation.ChatlogLine
import zel.game.moderation.RoomVisit
import zel.game.moderation.SupportTicket
import zel.game.moderation.TicketChatlogType
import zel.game.moderation.TicketType
import zel.protocol.generated.CfhChatlogOut
import zel.protocol.generated.PacketWriter
import zel.protocol.generated.PlayerChatlogOut
import zel.protocol.generated.RoomChatlogOut

object ModerationChatlogs {
    private const val ROOM_RECORD: Byte = 1
    private const val MAX_LINES = Short.MAX_VALUE.toInt()

    private sealed interface Header {
        val context: ChatRecordContext
    }

    private data class Text(
        override val context: ChatRecordContext,
        val value: String,
    ) : Header

    private data class Number(
        override val context: ChatRecordContext,
        val value: Int,
    ) : Header

    private fun roomHeader(
        roomName: String,
        roomId: Int,
    ) = listOf(Text(ChatRecordContext.ROOM_NAME, roomName), Number(ChatRecordContext.ROOM_ID, roomId))

    fun roomLog(
        roomName: String,
        roomId: Int,
        lines: List<ChatlogLine>,
    ): RoomChatlogOut {
        val record = PacketWriter()
        record.header(roomHeader(roomName, roomId))
        record.transcript(lines, withHighlights = false)
        return RoomChatlogOut(ROOM_RECORD, record.toByteArray())
    }

    fun playerLog(
        userId: Int,
        username: String,
        visits: List<RoomVisit>,
    ): PlayerChatlogOut {
        val records = PacketWriter().int(visits.size)
        for (visit in visits) {
            records.byte(ROOM_RECORD)
            records.header(roomHeader(visit.roomName, visit.roomId))
            records.transcript(visit.chat.sortedBy(ChatlogLine::timestamp), withHighlights = false)
        }
        return PlayerChatlogOut(userId, username, records.toByteArray())
    }

    fun ticketLog(
        ticket: SupportTicket,
        sortedLines: List<ChatlogLine>,
        roomName: String,
        type: TicketChatlogType,
        roomGuildId: Int,
    ): CfhChatlogOut? {
        if (sortedLines.isEmpty()) return null
        val header =
            when (ticket.type) {
                TicketType.MESSENGER -> {
                    listOf(Number(ChatRecordContext.MESSAGE_ID, ticket.reporterId))
                }

                TicketType.FORUM -> {
                    listOfNotNull(
                        Number(ChatRecordContext.GROUP_ID, ticket.guildId),
                        Number(ChatRecordContext.THREAD_ID, ticket.forumThreadId),
                        Number(ChatRecordContext.MESSAGE_ID, ticket.forumPostId)
                            .takeIf { type == TicketChatlogType.FORUM_REPLY },
                    )
                }

                TicketType.PHOTO -> {
                    listOf(
                        Text(ChatRecordContext.ROOM_NAME, roomName),
                        Number(ChatRecordContext.ROOM_ID, ticket.roomId),
                        Text(
                            ChatRecordContext.PHOTO_ID,
                            ticket.photo
                                ?.id
                                ?.toString()
                                .orEmpty(),
                        ),
                    )
                }

                else -> {
                    roomHeader(roomName, ticket.roomId) + Number(ChatRecordContext.GROUP_ID, roomGuildId)
                }
            }
        val record = PacketWriter()
        record.header(header)
        record.transcript(sortedLines, withHighlights = true)
        return CfhChatlogOut(
            ticket.id,
            ticket.reporterId,
            ticket.accusedId,
            ticket.roomId,
            type.code.toByte(),
            record.toByteArray(),
        )
    }

    private fun PacketWriter.header(values: List<Header>) {
        short(values.size.toShort())
        for (value in values) {
            string(value.context.key).byte(value.context.carries.code)
            when (value) {
                is Text -> {
                    check(value.context.carries == ChatRecordContext.Carries.TEXT) {
                        "${value.context.key} carries an int"
                    }
                    string(value.value)
                }

                is Number -> {
                    check(value.context.carries == ChatRecordContext.Carries.INT) {
                        "${value.context.key} carries text"
                    }
                    int(value.value)
                }
            }
        }
    }

    private fun PacketWriter.transcript(
        lines: List<ChatlogLine>,
        withHighlights: Boolean,
    ) {
        val shown = lines.take(MAX_LINES)
        short(shown.size.toShort())
        for (line in shown) {
            string(ChatLineTime.of(line.timestamp.toLong()))
            int(line.playerId)
            string(line.username)
            string(line.message)
            boolean(withHighlights && line.highlighted)
        }
    }
}
