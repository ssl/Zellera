package zel.net.server

import io.netty.channel.ChannelDuplexHandler
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelPromise
import io.netty.handler.codec.http.FullHttpRequest
import io.netty.handler.codec.http.HttpResponse

class BridgeAdmission(
    private val pool: WorkerPool,
    private val route: String,
) : ChannelDuplexHandler() {
    /** Confined to the socket's thread. */
    private var holdsSlot = false

    override fun channelRead(
        ctx: ChannelHandlerContext,
        msg: Any,
    ) {
        when {
            msg !is FullHttpRequest || msg.uri().substringBefore('?') != route || holdsSlot -> {
                ctx.fireChannelRead(msg)
            }

            pool.full -> {
                msg.release()
                HttpRefusal.BUSY.close(ctx)
            }

            else -> {
                holdsSlot = true
                pool.acquire()
                ctx.fireChannelRead(msg)
            }
        }
    }

    override fun write(
        ctx: ChannelHandlerContext,
        msg: Any,
        promise: ChannelPromise,
    ) {
        if (msg is HttpResponse) releaseSlot()
        ctx.write(msg, promise)
    }

    override fun channelInactive(ctx: ChannelHandlerContext) {
        releaseSlot()
        ctx.fireChannelInactive()
    }

    private fun releaseSlot() {
        if (!holdsSlot) return
        holdsSlot = false
        pool.release()
    }
}
