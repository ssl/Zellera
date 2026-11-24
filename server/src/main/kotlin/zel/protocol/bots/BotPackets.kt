package zel.protocol.bots

import zel.protocol.generated.BotInventoryOutCodec
import zel.protocol.generated.GetBotInventoryInCodec
import zel.protocol.typed.PacketPackage
import zel.protocol.typed.PacketRoute

class BotPackets : PacketPackage {
    override val name = "bots"

    override val routes =
        listOf(
            PacketRoute(GetBotInventoryInCodec) { session, _ ->
                val player = session.client.player ?: return@PacketRoute
                session.send(BotInventoryOutCodec, BotMessages.inventory(player))
            },
        )
}
