package zel.protocol.typed

import io.netty.buffer.ByteBuf
import zel.protocol.InboundFrame

/** A frame with the message its client's version means by the wire id; null when that version has no such id. */
class ClientPacket(
    wire: Int,
    val name: String?,
    body: ByteBuf?,
) : InboundFrame(wire, body)
