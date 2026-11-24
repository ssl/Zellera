package zel.protocol.typed

import io.netty.buffer.ByteBuf
import io.netty.buffer.DefaultByteBufHolder

/** An encoded frame; [name] is the message's, for the packet log. */
class WireFrame(
    frame: ByteBuf,
    val name: String,
) : DefaultByteBufHolder(frame) {
    override fun replace(content: ByteBuf): WireFrame = WireFrame(content, name)

    override fun retainedDuplicate(): WireFrame = replace(content().retainedDuplicate())
}
