package zel.protocol.incoming.moderation

import zel.game.moderation.SupportTicket
import zel.game.moderation.TicketState
import zel.game.players.Player
import zel.protocol.typed.PacketIds

object ModerationInput {
    const val MAX_MESSAGE_LENGTH = 500
    const val MAX_ROOM_CHAT_LOGS = 1000
    const val MAX_PRIVATE_CHAT_LOGS = 100
    const val MAX_TICKET_BATCH = 100
    private const val MAX_LINE_LENGTH = 500

    fun cleaned(text: String?): String = text.orEmpty().trim(' ').filterNot { it.isISOControl() }

    fun isMessage(text: String?): Boolean = !text.isNullOrEmpty() && text.length <= MAX_MESSAGE_LENGTH

    fun isTicketBatch(size: Int): Boolean = size in 1..MAX_TICKET_BATCH

    fun isRoomChatCount(size: Int): Boolean = size in 0..MAX_ROOM_CHAT_LOGS

    fun isPrivateChatCount(size: Int): Boolean = size in 1..MAX_PRIVATE_CHAT_LOGS

    fun isPrivateChatLine(text: String): Boolean = text.length <= MAX_LINE_LENGTH

    fun isPrivateChatAuthor(
        author: Int,
        reporterId: Int,
        reportedId: Int,
    ): Boolean = PacketIds.isId(author) && (author == reporterId || author == reportedId)

    fun ownsTicket(
        ticket: SupportTicket,
        moderator: Player,
    ): Boolean = ticket.state == TicketState.PICKED && ticket.moderatorId == moderator.profile.id

    fun withReporterContact(
        message: String,
        name: String?,
        email: String?,
    ): String {
        val contact = listOf(cleaned(name), cleaned(email)).filter { it.isNotEmpty() }
        val text = if (contact.isEmpty()) message else "$message [${contact.joinToString(", ")}]".trim()
        return text.take(MAX_MESSAGE_LENGTH)
    }
}
