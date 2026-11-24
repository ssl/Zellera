package zel.game.moderation

import zel.db.Params
import zel.db.Sql
import zel.db.allOrNothing
import zel.db.insertForId
import zel.db.orFallback
import zel.db.write
import zel.db.writeEach
import zel.game.items.OwnedFurni
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.sql.Connection
import java.sql.SQLException
import javax.sql.DataSource

/** Stand-in ids (0, -1) are stored as NULL and read back as they were. */
class SupportTicketStore(
    private val dataSource: DataSource,
) {
    private val queries = Sql(dataSource)

    fun insert(ticket: SupportTicket) {
        try {
            val id =
                dataSource.connection.use { connection ->
                    connection.allOrNothing {
                        connection.insertForId(INSERT) { facts(ticket) }?.also { id ->
                            storeLines(connection, ticket, id)
                            if (ticket.reporterId > 0) connection.write(COUNT_REPORT) { int(ticket.reporterId) }
                        }
                    }
                }
            id?.let { ticket.id = it }
        } catch (e: SQLException) {
            log.error("ticket of {} not stored", ticket.reporterId, e)
        }
    }

    fun update(ticket: SupportTicket) {
        try {
            dataSource.connection.use { connection ->
                connection.write(UPDATE) {
                    deskState(ticket)
                    int(ticket.id)
                }
            }
        } catch (e: SQLException) {
            log.error("ticket {} not updated", ticket.id, e)
        }
    }

    fun open(loadPhoto: (Int) -> OwnedFurni?): List<SupportTicket> {
        val desk =
            log.orFallback("open tickets", emptyList()) {
                queries.select(SELECT_ON_DESK).rows { SupportTicket.fromRow(it, loadPhoto) }
            }
        desk.forEach { it.pickedLines = linesOf(it.id).ifEmpty { null } }
        return desk
    }

    fun byId(
        id: Int,
        loadPhoto: (Int) -> OwnedFurni?,
    ): SupportTicket? {
        val ticket =
            log.orFallback("ticket $id", null) {
                queries.select(SELECT_ONE, id).first { SupportTicket.fromRow(it, loadPhoto) }
            }
        ticket?.pickedLines = linesOf(id).ifEmpty { null }
        return ticket
    }

    private fun linesOf(id: Int): List<ChatlogLine> =
        log.orFallback("lines of ticket $id", emptyList()) {
            queries.select(SELECT_LINES, id).rows { row ->
                ChatlogLine(
                    0,
                    row.getInt("player_id"),
                    row.getString("name").orEmpty(),
                    row.getString("message").orEmpty(),
                )
            }
        }

    private fun storeLines(
        connection: Connection,
        ticket: SupportTicket,
        ticketId: Int,
    ) {
        val lines = ticket.pickedLines.orEmpty()
        if (lines.isEmpty()) return
        connection.writeEach(INSERT_LINE, lines.withIndex()) { (no, pick) ->
            int(ticketId)
            int(no)
            int(pick.playerId)
            string(pick.message.take(MESSAGE_LENGTH))
        }
    }

    private fun Params.facts(ticket: SupportTicket) {
        int(ticket.type.code)
        string(ticket.state.stored)
        int(ticket.createdAt)
        int(ticket.priority)
        intOrNullWhenNotPositive(ticket.reporterId)
        intOrNull(ticket.accusedId, 0)
        intOrNull(ticket.roomId, 0)
        string(ticket.message.take(MESSAGE_LENGTH))
        int(ticket.category)
        intOrNull(ticket.guildId, SupportTicket.NO_PLACE)
        intOrNull(ticket.forumThreadId, SupportTicket.NO_PLACE)
        intOrNull(ticket.forumPostId, SupportTicket.NO_PLACE)
        intOrNullWhenNotPositive(ticket.photo?.id ?: 0)
    }

    private fun Params.deskState(ticket: SupportTicket) {
        string(ticket.state.stored)
        string(ticket.resolution?.stored)
        intOrNullWhenNotPositive(ticket.moderatorId)
        boolean(ticket.moderatorId == 0)
        int(ticket.category)
        intOrNullWhenNotPositive(ticket.closedAt)
        string(ticket.appeal.stored)
        intOrNullWhenNotPositive(ticket.appealResolvedAt)
    }

    private companion object {
        val log: Logger = LoggerFactory.getLogger(SupportTicketStore::class.java)
        const val MESSAGE_LENGTH = 500
        const val INSERT =
            "INSERT INTO support_tickets (kind, status, created_at, priority, reporter_id, accused_id, room_id, " +
                "message, topic_id, guild_id, forum_thread_id, forum_post_id, photo_furni_id) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"
        const val UPDATE =
            "UPDATE support_tickets SET status = ?, resolution = ?, moderator_id = ?, released = ?, topic_id = ?, " +
                "closed_at = ?, appeal = ?, appeal_resolved_at = ? WHERE id = ?"
        const val SELECT_TICKETS =
            "SELECT t.*, reporter.name AS reporter_name, accused.name AS accused_name, " +
                "moderator.name AS moderator_name " +
                "FROM support_tickets t LEFT JOIN players reporter ON reporter.id = t.reporter_id " +
                "LEFT JOIN players accused ON accused.id = t.accused_id " +
                "LEFT JOIN players moderator ON moderator.id = t.moderator_id "
        const val SELECT_ON_DESK = "${SELECT_TICKETS}WHERE t.status IN ('open', 'picked') ORDER BY t.id"
        const val SELECT_ONE = "${SELECT_TICKETS}WHERE t.id = ?"
        const val SELECT_LINES =
            "SELECT l.player_id, l.message, p.name FROM support_ticket_lines l " +
                "LEFT JOIN players p ON p.id = l.player_id WHERE l.ticket_id = ? ORDER BY l.line_no"
        const val INSERT_LINE =
            "INSERT INTO support_ticket_lines (ticket_id, line_no, player_id, message) VALUES (?, ?, ?, ?)"
        const val COUNT_REPORT = "UPDATE player_standing SET reports_sent = reports_sent + 1 WHERE player_id = ?"
    }
}
