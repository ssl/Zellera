package zel.protocol.outgoing.icons

import zel.game.icons.IconRoster
import zel.protocol.generated.IconShopDataOut
import zel.protocol.generated.IconShopDataOutCodec
import zel.protocol.outgoing.OutgoingMessage

class IconShopData(
    private val snapshot: IconRoster,
) : OutgoingMessage<IconShopDataOut>(IconShopDataOutCodec) {
    override fun message() = iconShopData(snapshot)
}
