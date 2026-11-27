package zel.game.players

import org.slf4j.LoggerFactory
import zel.db.SqlFailure
import zel.db.first
import zel.protocol.outgoing.handshake.DisconnectReason.Companion.CONCURRENT_LOGIN
import zel.server.GameServices

private val log = LoggerFactory.getLogger(PlayerLogin::class.java)

private const val LOGOUT_WAIT_MILLIS = 10_000L
private const val DEVICE_CHECK = "login.device.is_checked"
private const val TICKET_ALIVE = "a.login_ticket_expires_at >= UNIX_TIMESTAMP()"
private const val CONSUME_TICKET =
    "UPDATE player_accounts a SET login_ticket = NULL, login_ticket_expires_at = NULL, login_ticket_device = NULL " +
        "WHERE a.player_id = ? AND "
private const val PLAYER_SQL = "${PlayerAccountRow.SELECT} WHERE p.id = ?"
private const val BY_TICKET =
    "SELECT p.id, a.login_ticket_device FROM players p JOIN player_accounts a ON a.player_id = p.id WHERE "

/** One conditional update claims the ticket, so two logins never both pass. */
internal class PlayerLogin(
    private val services: GameServices,
    private val players: Players,
    private val disconnectSaves: DisconnectSaves,
) {
    private class Ticket(
        val playerId: Int,
        val device: DeviceReading?,
    )

    /** A ticket the device check refuses is not consumed: the browser it was made for can still use it. */
    fun logIn(
        sso: String,
        device: DeviceReading?,
    ): TicketLogin {
        if (sso.isBlank()) return TicketLogin.Unknown
        val honorExpiry = services.config.getBoolean("login.ticket.expires", true)
        val filter = "a.login_ticket = ?" + if (honorExpiry) " AND $TICKET_ALIVE" else ""
        val ticket = lookUp(BY_TICKET + filter, sso) ?: return TicketLogin.Unknown
        val checked = services.config.getBoolean(DEVICE_CHECK, true)
        val refused = if (checked) deviceRefusal(ticket.device, device) else null
        return refused ?: admitted(ticket.playerId, filter, sso, device.takeIf { checked }?.filledFrom(ticket.device))
    }

    private fun admitted(
        userId: Int,
        filter: String,
        sso: String,
        device: DeviceReading?,
    ): TicketLogin {
        val reusable = services.config.getBoolean("login.ticket.is_reusable", false)
        if (!reusable && !consumeSsoTicket(userId, filter, sso)) return TicketLogin.Unknown
        val player = loadPlayer(userId) ?: return TicketLogin.Unknown
        return TicketLogin.Admitted(player, device)
    }

    /** The device stored with the ticket against the one the client sent. */
    private fun deviceRefusal(
        stored: DeviceReading?,
        sent: DeviceReading?,
    ): TicketLogin.Refused? {
        if (stored == null || sent == null || !stored.sharesPieceWith(sent)) {
            return TicketLogin.Refused(TicketLogin.Reason.NO_DEVICE)
        }
        val differing = stored.differingFrom(sent)
        return if (differing.isEmpty()) null else TicketLogin.Refused(TicketLogin.Reason.DEVICE_MISMATCH, differing)
    }

    /** A session still online logs out on its own packet lane, so none of its packets writes after the reload. */
    private fun loadPlayer(userId: Int): Player? {
        awaitDisconnectPersistence(userId)
        services.clients.onlinePlayer(userId)?.let { online ->
            val older = online.client
            val loggedOut =
                older != null &&
                    services.clients.disconnectAndAwaitLogout(older, CONCURRENT_LOGIN, LOGOUT_WAIT_MILLIS)
            if (!loggedOut) online.disconnect()
            awaitDisconnectPersistence(userId)
        }
        return readPlayer(userId)
    }

    @Suppress("TooGenericExceptionCaught") // a failing login is logged and refused
    private fun readPlayer(userId: Int): Player? =
        try {
            services.database.dataSource.connection
                .use { connection -> connection.first(PLAYER_SQL, { int(userId) }, players::newPlayer) }
        } catch (e: Exception) {
            log.error("Player {} could not be loaded", userId, e)
            null
        }

    private fun lookUp(
        query: String,
        sso: String,
    ): Ticket? =
        try {
            services.database.queries
                .select(query, sso)
                .first { Ticket(it.getInt("id"), DeviceReading.parse(it.getString("login_ticket_device"))) }
        } catch (e: SqlFailure) {
            log.error("The login ticket could not be looked up", e)
            null
        }

    private fun awaitDisconnectPersistence(userId: Int) = disconnectSaves.await(userId)

    private fun consumeSsoTicket(
        userId: Int,
        filter: String,
        sso: String,
    ): Boolean =
        try {
            services.database.queries.write(CONSUME_TICKET + filter, userId, sso) == 1
        } catch (e: SqlFailure) {
            log.error("The login ticket of player {} could not be consumed", userId, e)
            false
        }
}
