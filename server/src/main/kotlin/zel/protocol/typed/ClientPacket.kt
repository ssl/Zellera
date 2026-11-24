package zel.protocol.typed

import zel.protocol.InboundFrame
import io.netty.buffer.ByteBuf

/** A frame with the message its client's version means by the wire id; null when that version has no such id. */
class ClientPacket(
    wire: Int,
    val name: String?,
    body: ByteBuf?,
) : InboundFrame(wire, body)
