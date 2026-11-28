package zel.protocol.handshake

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import zel.core.threading.GameThreads
import zel.ext.ExtensionBus
import zel.ext.ServerEvent
import zel.game.OpeningHours
import zel.game.OpeningHoursAccess
import zel.game.players.Player
import zel.game.players.Players
import zel.game.players.TicketLogin
import zel.net.clients.ClientConnection
import zel.net.clients.ClientConnections
import zel.net.server.proxy.ClientAddress
import zel.protocol.generated.LoginFailedGameClosedOut
import zel.protocol.generated.LoginFailedGameClosedOutCodec
import zel.protocol.outgoing.handshake.DisconnectReason
import zel.security.Suspicion
import zel.security.SuspicionLog

/** A newer login of the player replaces the older. */
class PlayerSessions(
    private val clients: ClientConnections,
    private val players: Players,
    private val access: OpeningHoursAccess,
    private val threading: GameThreads? = null,
    private val openingHours: OpeningHours = OpeningHours(),
    private val crowd: LoginCrowd = LoginCrowd(null),
    private val suspicions: SuspicionLog = SuspicionLog.None,
) {
    fun start(
        client: ClientConnection,
        ticket: String,
    ): Player? {
        val login = players.logIn(ticket, client.device, ClientAddress.of(client.channel))
        if (login !is TicketLogin.Admitted) {
            if (login is TicketLogin.Refused) turnAway(client, login) else log.warn(UNKNOWN_TICKET)
            clients.failedTickets.record(ClientAddress.of(client.channel))
            clients.close(client)
            return null
        }
        val player = login.player
        val from = ClientAddress.of(client.channel)
        if (crowd.refuses(player.profile.id, login.device, from, players.all)) {
            log.info(
                "login of player {} from {} turned away: too many players online from there",
                player.profile.id,
                from,
            )
            login.release()
            clients.close(client)
            return null
        }
        try {
            player.device = login.device
            clients.claim(player.profile.id, client)?.takeIf { it !== client }?.let { older ->
                log.info("Player {} logs in again; the older session is closed", player.profile.id)
                clients.disconnect(older, DisconnectReason.CONCURRENT_LOGIN)
            }
            if (!enter(client, player)) return null
        } finally {
            login.release()
        }
        login.device?.let { players.loggedInFrom(player, it) }
        ExtensionBus.event(ServerEvent.LoggedIn(player.profile.id))
        return player
    }

    /** Names the pieces that differ, never what they hold. */
    private fun turnAway(
        client: ClientConnection,
        refused: TicketLogin.Refused,
    ) {
        val differing = refused.differing
        val pieces = if (differing.isEmpty()) "" else differing.joinToString(prefix = " (", postfix = ")")
        val from = ClientAddress.of(client.channel)
        log.info("login from {} turned away: {}{}", from, refused.reason, pieces)
        if (refused.reason == TicketLogin.Reason.TICKET_ADDRESS) {
            suspicions.record(refused.playerId, from, client.device?.usable("id"), Suspicion.TICKET_ADDRESS)
        }
    }

    @Suppress("TooGenericExceptionCaught") // any failure closes the connection
    private fun enter(
        client: ClientConnection,
        player: Player,
    ): Boolean =
        try {
            player.client = client
            client.player = player
            when {
                !player.connect() -> {
                    refuse(client)
                }

                !openingHours.isOpen && !access.letsThrough(player.profile.rank.id) -> {
                    client.send(
                        LoginFailedGameClosedOutCodec,
                        LoginFailedGameClosedOut(openingHours.openHour, openingHours.openMinute),
                    )
                    refuse(client)
                }

                else -> {
                    // a machine fingerprint that arrived before the ticket lets machine bans target this player
                    client.machineId.takeIf { it.isNotEmpty() }?.let { player.profile.machineId = it }
                    threading?.run(player)
                    players.register(player)
                    true
                }
            }
        } catch (e: Exception) {
            log.error("Login of player {} failed", player.profile.id, e)
            refuse(client)
        }

    private fun refuse(client: ClientConnection): Boolean {
        clients.close(client)
        return false
    }

    private companion object {
        val log: Logger = LoggerFactory.getLogger(PlayerSessions::class.java)
        const val UNKNOWN_TICKET = "Login with an unknown ticket; connection closed"
    }
}
