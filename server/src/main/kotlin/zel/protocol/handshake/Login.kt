package zel.protocol.handshake

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import zel.core.Texts
import zel.net.clients.ClientConnection
import zel.net.clients.ClientConnections
import zel.net.server.proxy.ClientAddress
import zel.protocol.outgoing.generic.alerts.Broadcast
import zel.security.ClientCheck
import zel.security.Suspicion
import zel.security.SuspicionLog

class Login(
    private val gate: LoginAdmission,
    private val sessions: PlayerSessions,
    private val burst: LoginBurst,
    private val followUps: LoginFollowUps,
    private val connections: ClientConnections,
    private val texts: Texts,
    private val suspicions: SuspicionLog = SuspicionLog.None,
    private val clientCheck: ClientCheck? = null,
) {
    fun admit(
        client: ClientConnection,
        rawTicket: String,
    ) {
        if (client.player != null) return turnAway(client, "SECOND_TICKET")
        gate.refusal(client)?.let { refusal ->
            explain(client, refusal)
            return turnAway(client, refusal.name)
        }
        val ticket = ticketOf(client, rawTicket) ?: return
        val player = sessions.start(client, ticket) ?: return
        burst.sendTo(client, player)
        followUps.run(client, player)
        clientCheck?.loggedIn(client)
    }

    /** Null after turning the client away: its address failed too often, or this is no ticket. */
    private fun ticketOf(
        client: ClientConnection,
        rawTicket: String,
    ): String? {
        val from = ClientAddress.of(client.channel)
        val tooMany = connections.failedTickets.refuses(from)
        val ticket = if (tooMany) null else SsoTicket.accepted(rawTicket)
        if (ticket == null) {
            if (!tooMany) connections.failedTickets.record(from)
            turnAway(client, if (tooMany) "TOO_MANY_FAILED_TICKETS" else "MALFORMED_TICKET")
        }
        return ticket
    }

    private fun explain(
        client: ClientConnection,
        refusal: LoginAdmission.Refusal,
    ) {
        if (refusal == LoginAdmission.Refusal.OVERLOADED) client.send(Broadcast(texts.textOr(BUSY_KEY, BUSY_NOTICE)))
        if (refusal == LoginAdmission.Refusal.RELEASE) {
            log.warn("client release '{}' is not in '{}'", client.releaseName, gate.allowedReleases())
            val from = ClientAddress.of(client.channel)
            suspicions.record(
                null,
                from,
                client.device?.usable("id"),
                Suspicion.RELEASE,
                client.releaseName.take(DETAIL),
            )
        }
    }

    private fun turnAway(
        client: ClientConnection,
        why: String,
    ) {
        log.info("login from {} turned away: {}", ClientAddress.of(client.channel), why)
        connections.close(client)
    }

    private companion object {
        val log: Logger = LoggerFactory.getLogger(Login::class.java)
        const val BUSY_KEY = "login.busy"
        const val BUSY_NOTICE = "The game is temporarily busy. Please try again."
        const val DETAIL = 80
    }
}
