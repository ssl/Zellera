package zel.net.server

import io.netty.channel.ChannelDuplexHandler
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelPromise
import zel.protocol.InboundFrame
import zel.protocol.typed.ClientVersions
import zel.protocol.typed.WireFrame

class ShuffleStage(
    private val shuffle: PacketShuffle,
    private val strikesAllowed: Int,
    private val unknown: (wire: Int, strikes: Int) -> Unit,
) : ChannelDuplexHandler() {
    private var strikes = 0

    override fun channelRead(
        ctx: ChannelHandlerContext,
        msg: Any,
    ) {
        if (msg !is InboundFrame) {
            ctx.fireChannelRead(msg)
            return
        }
        val id = shuffle.opened(msg.messageId)
        val version = ClientVersions.of(ctx.channel())
        if (version != null && version.incoming(id) == null) {
            strikes++
            unknown(msg.messageId and MASK, strikes)
            if (strikesAllowed in 1..strikes) {
                msg.release()
                ctx.close()
                return
            }
        }
        ctx.fireChannelRead(InboundFrame(id, msg.body))
    }

    override fun write(
        ctx: ChannelHandlerContext,
        msg: Any,
        promise: ChannelPromise,
    ) {
        val frame = msg as? WireFrame
        val bytes = frame?.content()
        if (frame == null || bytes == null || bytes.readableBytes() < HEADER_BYTES) {
            ctx.write(msg, promise)
            return
        }
        val copy =
            ctx
                .alloc()
                .buffer(
                    bytes.readableBytes(),
                ).writeBytes(bytes, bytes.readerIndex(), bytes.readableBytes())
        copy.setShort(LENGTH_BYTES, shuffle.sealed(copy.getUnsignedShort(LENGTH_BYTES)))
        frame.release()
        ctx.write(WireFrame(copy, frame.name), promise)
    }

    private companion object {
        const val MASK = 0xFFFF
        const val LENGTH_BYTES = 4
        const val HEADER_BYTES = 6
    }
}
