package zel.protocol.typed

import zel.net.clients.ClientConnection
import zel.protocol.InboundFrame

class PacketDispatcher(
    packages: List<PacketPackage>,
    allowed: AllowedValues = AllowedValues(),
    private val shuttingDown: () -> Boolean = { false },
) {
    val router = PacketRouter(packages, allowed)

    fun handle(
        client: ClientConnection?,
        message: InboundFrame,
    ) {
        if (client == null || client.isClosed || shuttingDown()) return
        if (message is ClientPacket) router.dispatch(client, message)
    }

    companion object {
        @Volatile
        var showPackets: Boolean = false
    }
}
