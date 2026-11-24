package zel.protocol.outgoing.nux

import zel.protocol.generated.InClientLinkOut
import zel.protocol.generated.InClientLinkOutCodec
import zel.protocol.outgoing.OutgoingMessage

class TourHelpBubble(
    control: String,
    text: String,
) : OutgoingMessage<InClientLinkOut>(InClientLinkOutCodec) {
    val link = "helpBubble/add/$control/$text"

    override fun message() = InClientLinkOut(link)
}
