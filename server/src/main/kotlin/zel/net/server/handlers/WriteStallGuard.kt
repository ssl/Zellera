package zel.net.server.handlers

import io.netty.channel.ChannelDuplexHandler
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelPromise
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.concurrent.TimeUnit

/**
 * Watches a connection whose unsent output is over the pause size, once a second. A client that still takes
 * output, however slowly, stays; it closes after [stalledSeconds] in which nothing left, or when more than
 * [backlogBytes] wait.
 */
class WriteStallGuard(
    private val stalledSeconds: Int,
    private val backlogBytes: Long,
) : ChannelDuplexHandler() {
    init {
        require(stalledSeconds > 0) { "a stall lasts a positive time, not $stalledSeconds seconds" }
        require(backlogBytes > 0) { "the backlog limit must be positive, not $backlogBytes" }
    }

    // Confined to the connection's event loop.
    private var watching = false
    private var sentSinceLook = false
    private var backlogAtLook = 0L
    private var idleLooks = 0

    override fun channelWritabilityChanged(ctx: ChannelHandlerContext) {
        if (ctx.channel().isWritable) {
            idleLooks = 0
        } else if (!watching) {
            watching = true
            sentSinceLook = false
            backlogAtLook = backlog(ctx)
            bookLook(ctx)
        }
        ctx.fireChannelWritabilityChanged()
    }

    override fun write(
        ctx: ChannelHandlerContext,
        msg: Any,
        promise: ChannelPromise,
    ) {
        if (!watching) {
            ctx.write(msg, promise)
            return
        }
        ctx.write(msg, promise.unvoid().addListener { if (it.isSuccess) sentSinceLook = true })
    }

    private fun bookLook(ctx: ChannelHandlerContext) {
        ctx.executor().schedule({ look(ctx) }, 1, TimeUnit.SECONDS)
    }

    private fun look(ctx: ChannelHandlerContext) {
        val channel = ctx.channel()
        if (!channel.isActive || channel.isWritable) {
            watching = false
            return
        }
        val backlog = backlog(ctx)
        idleLooks = if (sentSinceLook || backlog < backlogAtLook) 0 else idleLooks + 1
        sentSinceLook = false
        backlogAtLook = backlog
        when {
            backlog > backlogBytes -> drop(ctx, "$backlog bytes of output wait for it")
            idleLooks >= stalledSeconds -> drop(ctx, "it took no output for $stalledSeconds seconds")
            else -> bookLook(ctx)
        }
    }

    private fun backlog(ctx: ChannelHandlerContext): Long =
        ctx
            .channel()
            .unsafe()
            .outboundBuffer()
            ?.totalPendingWriteBytes() ?: 0L

    private fun drop(
        ctx: ChannelHandlerContext,
        why: String,
    ) {
        watching = false
        log.warn("closing {}: {}", ctx.channel(), why)
        ctx.close()
    }

    private companion object {
        val log: Logger = LoggerFactory.getLogger(WriteStallGuard::class.java)
    }
}
