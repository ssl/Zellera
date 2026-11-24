package zel.game.moderation

import zel.core.threading.GameThreads
import zel.game.items.OwnedFurni
import zel.game.players.Player
import zel.game.players.connection
import zel.protocol.outgoing.moderation.ModerationOutbox
import zel.protocol.outgoing.moderation.ModerationReplies
import java.util.concurrent.ConcurrentHashMap
import javax.sql.DataSource

/** The tickets waiting for or with a moderator. Shared by every packet thread; changes are stored on the writers. */
@Suppress("LongParameterList") // the services a ticket's life touches
class TicketDesk(
    dataSource: DataSource,
    private val threading: GameThreads,
    private val outbox: ModerationOutbox,
    private val standing: StandingCounters,
    private val loadPhoto: (Int) -> OwnedFurni?,
    private val clock: () -> Int,
) {
    private val ticketStore = SupportTicketStore(dataSource)
    private val tickets = ConcurrentHashMap<Int, SupportTicket>()

    init {
        ticketStore.open(loadPhoto).forEach { tickets[it.id] = it }
    }

    fun open(): List<SupportTicket> = tickets.values.sortedBy { it.id }

    fun byId(id: Int): SupportTicket? = tickets[id]

    /** The player's own calls no moderator has picked yet: the ones the player may still withdraw. */
    fun openOf(player: Player): List<SupportTicket> =
        open().filter { it.reporterId == player.profile.id && it.state == TicketState.OPEN }

    fun filedBy(playerId: Int): List<SupportTicket> = open().filter { it.reporterId == playerId }

    /** True while [moderatorId] holds a ticket [reporterId] filed: what they say to the reporter answers it. */
    fun isAnswering(
        moderatorId: Int,
        reporterId: Int,
    ): Boolean =
        tickets.values.any {
            it.state == TicketState.PICKED && it.moderatorId == moderatorId && it.reporterId == reporterId
        }

    @Suppress("LongParameterList") // the facts of one report
    fun draft(
        type: TicketType,
        reporterId: Int,
        reporterName: String,
        accusedId: Int,
        accusedName: String,
        roomId: Int,
        message: String,
    ): SupportTicket = SupportTicket(type, clock(), reporterId, reporterName, accusedId, accusedName, roomId, message)

    /** Stores a new ticket and puts it on the desk; false when it could not be stored. */
    fun file(ticket: SupportTicket): Boolean {
        if (ticket.id == 0) ticketStore.insert(ticket)
        if (ticket.id <= 0) return false
        tickets[ticket.id] = ticket
        announce(ticket)
        return true
    }

    fun save(ticket: SupportTicket) = threading.write { ticketStore.update(ticket) }

    fun announce(ticket: SupportTicket) = outbox.issueInfoToQueue(ticket, clock())

    /** Gives an open ticket to [moderator]; false when another moderator picked it first or it is closed. */
    fun pick(
        ticket: SupportTicket,
        moderator: Player,
    ): Boolean {
        synchronized(ticket) {
            if (ticket.state != TicketState.OPEN || tickets[ticket.id] !== ticket) return false
            ticket.state = TicketState.PICKED
            ticket.moderatorId = moderator.profile.id
            ticket.moderatorName = moderator.profile.name
        }
        save(ticket)
        announce(ticket)
        return true
    }

    fun release(
        ticket: SupportTicket,
        moderator: Player,
    ): Boolean {
        synchronized(ticket) {
            if (ticket.state != TicketState.PICKED || ticket.moderatorId != moderator.profile.id) return false
            ticket.moderatorId = 0
            ticket.moderatorName = ""
            ticket.state = TicketState.OPEN
        }
        save(ticket)
        announce(ticket)
        return true
    }

    /**
     * Takes the ticket off the desk for [resolution] and tells the reporter, if online, what a moderator decided;
     * a report judged abusive counts against its reporter. False when it was closed already.
     */
    fun close(
        ticket: SupportTicket,
        resolution: TicketResolution,
    ): Boolean {
        if (tickets.remove(ticket.id) == null) return false
        synchronized(ticket) {
            ticket.state = TicketState.CLOSED
            ticket.closedAt = clock()
            ticket.resolution = resolution
            if (ticket.appeal == FiledReport.Appeal.PENDING) {
                val upheld = resolution == TicketResolution.HANDLED
                ticket.appeal = if (upheld) FiledReport.Appeal.ACTED_ON else FiledReport.Appeal.NOT_ACTED_ON
                ticket.appealResolvedAt = ticket.closedAt
            }
        }
        save(ticket)
        announce(ticket)
        outbox.issueDeletedToQueue(ticket.id)
        if (resolution == TicketResolution.ABUSIVE && ticket.reporterId > 0) {
            standing.count(ticket.reporterId, StandingCounter.REPORTS_ABUSIVE)
        }
        return true
    }

    /** The close notice the reporter's client shows for a moderator's decision; null for the closings without one. */
    fun noticeFor(resolution: TicketResolution): Int? =
        when (resolution) {
            TicketResolution.USELESS -> ModerationReplies.USELESS
            TicketResolution.ABUSIVE -> ModerationReplies.ABUSIVE
            TicketResolution.HANDLED -> ModerationReplies.HANDLED
            TicketResolution.WITHDRAWN, TicketResolution.AUTOMATIC -> null
        }

    fun tellReporter(
        reporter: Player?,
        resolution: TicketResolution,
    ) {
        val notice = noticeFor(resolution) ?: return
        reporter?.let { ModerationReplies.issueHandled(it.connection, notice) }
    }

    /** Puts an appealed closed ticket back in the queue unpicked; its next close settles the appeal. */
    fun reopenForAppeal(id: Int): Boolean {
        if (tickets.containsKey(id)) return false
        val ticket = ticketStore.byId(id, loadPhoto) ?: return false
        if (ticket.state != TicketState.CLOSED || ticket.appeal != FiledReport.Appeal.PENDING) return false
        ticket.state = TicketState.OPEN
        ticket.resolution = null
        ticket.moderatorId = SupportTicket.UNPICKED
        ticket.moderatorName = null
        if (tickets.putIfAbsent(ticket.id, ticket) != null) return false
        save(ticket)
        announce(ticket)
        return true
    }
}
