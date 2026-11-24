package zel.net.server.encoders

import io.netty.buffer.ByteBuf
import io.netty.buffer.ByteBufUtil
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelOutboundHandlerAdapter
import io.netty.channel.ChannelPromise
import zel.net.server.PacketDump
import zel.protocol.typed.WireFrame

class GameFrameEncoder : ChannelOutboundHandlerAdapter() {
    override fun write(
        ctx: ChannelHandlerContext,
        msg: Any,
        promise: ChannelPromise,
    ) {
        val bytes = (msg as? WireFrame)?.content()
        if (bytes != null && PacketDump.enabled) dump(bytes)
        ctx.write(bytes ?: msg, promise)
    }

    private fun dump(frame: ByteBuf) {
        if (frame.readableBytes() < HEADER_BYTES) return
        val at = frame.readerIndex()
        val wire = frame.getUnsignedShort(at + LENGTH_BYTES)
        val body = ByteBufUtil.hexDump(frame, at + HEADER_BYTES, frame.readableBytes() - HEADER_BYTES)
        PacketDump.line("s2c $wire 0 $body")
    }

    private companion object {
        const val LENGTH_BYTES = 4
        const val HEADER_BYTES = 6
    }
}
