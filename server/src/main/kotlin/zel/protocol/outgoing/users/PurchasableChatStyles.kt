package zel.protocol.outgoing.users

import zel.protocol.generated.PurchasableChatStylesOut
import zel.protocol.generated.PurchasableChatStylesOutCodec
import zel.protocol.outgoing.OutgoingMessage

class PurchasableChatStyles(
    styleIds: Collection<Int>?,
) : OutgoingMessage<PurchasableChatStylesOut>(PurchasableChatStylesOutCodec) {
    val styleIds: List<Int> = styleIds?.toList().orEmpty()

    override fun message() = PurchasableChatStylesOut(styleIds)
}
