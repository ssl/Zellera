package zel.protocol.incoming.guilds

import zel.game.players.Player
import zel.net.clients.ClientConnection
import zel.protocol.generated.IncomingCodec
import zel.protocol.typed.ClientCooldown
import zel.protocol.typed.PacketRoute

internal class Caller(
    val client: ClientConnection,
    val player: Player,
) {
    val userId: Int get() = player.profile.id
}

internal fun <T> callerRoute(
    codec: IncomingCodec<T>,
    gapMs: Long = 0,
    handler: (Caller, T) -> Unit,
): PacketRoute<T> {
    val cooldown = ClientCooldown(codec.name, gapMs)
    return PacketRoute(codec) { session, message ->
        val client = session.client
        val player = client.player
        if (player != null && (gapMs <= 0 || cooldown.acquire(client))) handler(Caller(client, player), message)
    }
}
