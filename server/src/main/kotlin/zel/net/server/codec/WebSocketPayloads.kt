package zel.net.server.codec

import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelDuplexHandler
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelPromise
import io.netty.handler.codec.http.HttpRequest
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame
import io.netty.handler.codec.http.websocketx.WebSocketFrame
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler.HandshakeComplete
import io.netty.util.ReferenceCountUtil
import zel.net.server.HttpRefusal

class WebSocketPayloads(
    private val httpStages: List<String>,
) : ChannelDuplexHandler() {
    // A frame's payload carries the frame's one reference on, so nothing is retained or released for it.
    override fun channelRead(
        ctx: ChannelHandlerContext,
        msg: Any,
    ) {
        when {
            msg is BinaryWebSocketFrame && msg.isFinalFragment -> {
                ctx.fireChannelRead(msg.content())
            }

            msg is WebSocketFrame -> {
                msg.release()
                ctx.close()
            }

            msg is HttpRequest -> {
                ReferenceCountUtil.release(msg)
                HttpRefusal.NOT_FOUND.close(ctx)
            }

            else -> {
                ctx.fireChannelRead(msg)
            }
        }
    }

    override fun write(
        ctx: ChannelHandlerContext,
        msg: Any,
        promise: ChannelPromise,
    ) {
        ctx.write(if (msg is ByteBuf) BinaryWebSocketFrame(msg) else msg, promise)
    }

    override fun userEventTriggered(
        ctx: ChannelHandlerContext,
        evt: Any,
    ) {
        if (evt is HandshakeComplete) {
            val pipeline = ctx.pipeline()
            for (stage in httpStages) pipeline.context(stage)?.let { pipeline.remove(it.handler()) }
        }
        ctx.fireUserEventTriggered(evt)
    }
}
