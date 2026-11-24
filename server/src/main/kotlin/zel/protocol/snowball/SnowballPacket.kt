package zel.protocol.snowball

import zel.net.clients.ClientConnection
import zel.protocol.generated.OutgoingCodec
import zel.protocol.generated.PacketWriter
import zel.protocol.typed.PacketSession

class SnowballPacket<T : Any>(
    val codec: OutgoingCodec<T>,
    val message: T,
) {
    fun body(): ByteArray = PacketWriter().also { codec.write(it, message) }.toByteArray()

    fun sendTo(client: ClientConnection) = client.send(codec, message)
}

fun <T : Any> PacketSession.send(packet: SnowballPacket<T>) = send(packet.codec, packet.message)
