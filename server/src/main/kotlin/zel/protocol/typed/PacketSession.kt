package zel.protocol.typed

import zel.net.clients.ClientConnection
import zel.protocol.generated.OutgoingCodec

interface PacketSession {
    val client: ClientConnection

    fun <T> send(
        codec: OutgoingCodec<T>,
        message: T,
    )
}
