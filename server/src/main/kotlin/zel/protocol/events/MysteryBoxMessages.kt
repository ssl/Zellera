package zel.protocol.events

import zel.net.clients.ClientConnection
import zel.protocol.generated.GotMysteryBoxPrizeOut
import zel.protocol.generated.GotMysteryBoxPrizeOutCodec

class MysteryBoxMessages {
    fun prize(
        client: ClientConnection,
        type: String,
        itemId: Int,
    ) = client.send(GotMysteryBoxPrizeOutCodec, GotMysteryBoxPrizeOut(type, itemId))
}
