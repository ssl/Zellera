package zel.net.clients

import zel.game.players.Player
import zel.net.crypto.DiffieHellman
import zel.protocol.Outbound
import zel.protocol.generated.OutgoingCodec
import zel.protocol.outgoing.handshake.DisconnectReason
import io.netty.channel.Channel
import io.netty.channel.ChannelId
import java.util.concurrent.ConcurrentHashMap

class ClientConnections(
    private val newHandshake: () -> DiffieHellman? = { null },
    private val maxOpen: Int = Int.MAX_VALUE,
    val failedTickets: FailedTickets = FailedTickets({ 0 }, { 1 }),
) {
    private val open = ConcurrentHashMap<ChannelId, ClientConnection>()
    private val owners = ConcurrentHashMap<Int, ClientConnection>()

    val connections: Collection<ClientConnection> get() = open.values

    /** False when [maxOpen] connections are open or the channel is already tracked. */
    fun open(channel: Channel): Boolean {
        if (open.size >= maxOpen) return false
        val connection = ClientConnection(channel, newHandshake)
        channel.closeFuture().addListener { forget(channel) }
        channel.attr(ClientConnection.KEY).set(connection)
        return track(connection)
    }

    internal fun track(connection: ClientConnection): Boolean =
        open.putIfAbsent(connection.channel.id(), connection) == null

    fun close(connection: ClientConnection?) {
        connection?.let { forget(it.channel) }
    }

    fun disconnect(
        connection: ClientConnection?,
        reason: Int,
    ) {
        if (connection == null) return
        if (connection.channel.isActive) connection.send(DisconnectReason(reason))
        close(connection)
    }

    fun disconnectAndAwaitLogout(
        connection: ClientConnection,
        reason: Int,
        timeoutMillis: Long,
    ): Boolean {
        if (connection.channel.isActive) connection.send(DisconnectReason(reason))
        val loggedOut = connection.closeAndAwaitLogout(timeoutMillis)
        close(connection)
        return loggedOut
    }

    fun claim(
        playerId: Int,
        connection: ClientConnection,
    ): ClientConnection? = if (playerId <= 0) null else owners.put(playerId, connection)

    fun release(
        playerId: Int,
        connection: ClientConnection,
    ) {
        if (playerId > 0) owners.remove(playerId, connection)
    }

    fun owner(playerId: Int): ClientConnection? = if (playerId <= 0) null else owners[playerId]

    fun onlinePlayer(id: Int): Player? =
        owners[id]?.player?.takeIf { it.profile.id == id }
            ?: firstPlayer { it.profile.id == id }

    fun onlinePlayer(username: String): Player? = firstPlayer { it.profile.name.equals(username, ignoreCase = true) }

    fun <T> broadcast(
        codec: OutgoingCodec<T>,
        message: T,
    ) {
        val outbound = Outbound(codec, message)
        open.values.forEach { it.send(outbound) }
    }

    private fun forget(channel: Channel) {
        channel.attr(ClientConnection.KEY).getAndSet(null)?.let { connection ->
            connection.player?.profile?.let { release(it.id, connection) }
            connection.close()
        }
        channel.close()
        open.remove(channel.id())
    }

    private fun firstPlayer(match: (Player) -> Boolean): Player? =
        open.values.firstNotNullOfOrNull { connection -> connection.player?.takeIf(match) }
}
