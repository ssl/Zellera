package zel.room.tick

import io.netty.channel.Channel
import zel.ext.ExtensionBus
import zel.net.clients.ClientConnections
import zel.protocol.Outbound
import zel.protocol.generated.OutgoingCodec
import zel.protocol.typed.ClientVersion
import zel.protocol.typed.ClientVersions
import zel.protocol.typed.WireFrame

class ClientChannels(
    private val clients: ClientConnections,
) : PlayerChannels {
    override fun <T> write(
        playerIds: List<Int>,
        codec: OutgoingCodec<T>,
        message: T,
    ) {
        val outbound = Outbound(codec, message)
        val hooked = ExtensionBus.hooksOutgoing(codec.name)
        val frames = HashMap<ClientVersion, WireFrame?>()
        try {
            for (id in playerIds) {
                val channel = channelOf(id)
                val version = channel?.let { ClientVersions.of(it) }
                val mine = if (hooked) ExtensionBus.outgoing(id, codec, message) else message
                if (channel == null || version == null || mine == null) continue
                val frame =
                    if (mine !== message) {
                        Outbound(codec, mine).frame(channel.alloc(), version)
                    } else {
                        frames.getOrPut(version) { outbound.frame(channel.alloc(), version) }?.retainedDuplicate()
                    }
                frame?.let { channel.write(it, channel.voidPromise()) }
            }
        } finally {
            frames.values.forEach { it?.release() }
        }
    }

    override fun flush(playerIds: Collection<Int>) {
        playerIds.forEach { channelOf(it)?.flush() }
    }

    private fun channelOf(playerId: Int): Channel? = clients.owner(playerId)?.channel?.takeIf { it.isOpen }
}
