package zel.game.players

import zel.game.moderation.PlayerBanning
import zel.net.clients.ClientConnection
import zel.net.server.proxy.ClientAddress
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.net.InetSocketAddress

/** Refuses until moderation runs, then checks account, machine, address and device bans in that order. */
internal class LoginGate(
    private val player: Player,
) {
    private val client: ClientConnection? = player.client

    /**
     * The proxy's forwarded address, else the socket's; empty when `network.client_address.is_proxy_only` is on and
     * the proxy forwarded none.
     */
    val address: String = client?.let(::addressOf).orEmpty()

    private val bars: List<(PlayerBanning, ClientConnection) -> Boolean> =
        listOf(::accountBanned, ::machineBanned, { bans, _ -> bans.hasIpBan(address) }, ::deviceBanned)

    fun passes(): Boolean {
        val connection = client ?: return false
        val bans =
            player.services.game.moderationService
                ?.banning ?: return false
        return bars.none { barred -> barred(bans, connection) }
    }

    private fun accountBanned(
        bans: PlayerBanning,
        connection: ClientConnection,
    ): Boolean {
        val ban = bans.checkForBan(player.profile.id) ?: return false
        connection.send(bans.banInfo(ban))
        return true
    }

    private fun machineBanned(
        bans: PlayerBanning,
        connection: ClientConnection,
    ): Boolean = bans.hasMachineBan(connection)

    /** Closed without a word, as for a machine ban; the log names the rule, never what a piece holds. */
    private fun deviceBanned(
        bans: PlayerBanning,
        connection: ClientConnection,
    ): Boolean {
        val device = player.device ?: return false
        val rule = bans.deviceBan(player.profile.id, device) ?: return false
        val from = connection.channel.remoteAddress()
        log.info("login of player {} from {} turned away: DEVICE_BANNED ({})", player.profile.id, from, rule)
        return true
    }

    private fun addressOf(connection: ClientConnection): String {
        val channel = connection.channel
        channel.attr(ClientAddress.FORWARDED).get()?.let { return it }
        if (player.services.config.getBoolean("network.client_address.is_proxy_only")) return ""
        return (channel.remoteAddress() as? InetSocketAddress)?.address?.hostAddress.orEmpty()
    }

    private companion object {
        val log: Logger = LoggerFactory.getLogger(LoginGate::class.java)
    }
}
