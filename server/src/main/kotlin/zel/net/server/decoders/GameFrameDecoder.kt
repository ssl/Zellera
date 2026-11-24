package zel.net.server.decoders

import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.ByteToMessageDecoder
import zel.net.WireLimits
import zel.net.clients.ClientConnection
import zel.protocol.InboundFrame
import zel.protocol.typed.ClientVersions

/**
 * Splits the inbound byte stream into `int length, short id, body` frames. A frame over [ordinaryFrameBytes] is
 * refused unless its message is one of [largeFrames], before its body is waited for.
 */
class GameFrameDecoder(
    private val maxFrameBytes: Int = WireLimits().maxFrameBytes,
    private val ordinaryFrameBytes: Int = WireLimits().ordinaryFrameBytes,
    private val largeFrames: Set<String> = WireLimits().largeFrames,
) : ByteToMessageDecoder() {
    constructor(limits: WireLimits) : this(limits.maxFrameBytes, limits.ordinaryFrameBytes, limits.largeFrames)

    private var broken = false

    override fun decode(
        ctx: ChannelHandlerContext,
        input: ByteBuf,
        out: MutableList<Any>,
    ) {
        while (!broken && input.readableBytes() >= Int.SIZE_BYTES) {
            val length = input.getInt(input.readerIndex())
            val problem =
                when {
                    length < 0 || length > maxFrameBytes -> "bad frame length"
                    length < Short.SIZE_BYTES -> "shorter than a header"
                    length > ordinaryFrameBytes && !mayBeLarge(ctx, input) -> "too long for its message"
                    else -> null
                }
            if (problem != null) {
                reject(ctx, input, problem)
                return
            }
            if (input.readableBytes() < Int.SIZE_BYTES + length) return
            input.skipBytes(Int.SIZE_BYTES)
            val id = input.readShort().toInt()
            out += InboundFrame(id, input.readBytes(length - Short.SIZE_BYTES))
        }
        if (broken) input.skipBytes(input.readableBytes())
    }

    /** Until its id has arrived a frame may still be one of [largeFrames]. */
    private fun mayBeLarge(
        ctx: ChannelHandlerContext,
        input: ByteBuf,
    ): Boolean =
        input.readableBytes() < HEADER_BYTES ||
            message(ctx, input.getUnsignedShort(input.readerIndex() + Int.SIZE_BYTES)) in largeFrames

    private fun message(
        ctx: ChannelHandlerContext,
        wire: Int,
    ): String? = ClientVersions.of(ctx.channel())?.incoming(wire)

    private fun reject(
        ctx: ChannelHandlerContext,
        input: ByteBuf,
        problem: String,
    ) {
        broken = true
        input.skipBytes(input.readableBytes())
        ctx
            .channel()
            .attr(ClientConnection.KEY)
            .get()
            ?.warnings
            ?.invalid("frame", problem)
        ctx.close()
    }

    private companion object {
        const val HEADER_BYTES = Int.SIZE_BYTES + Short.SIZE_BYTES
    }
}
