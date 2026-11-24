package zel.net.server

import zel.net.WireLimits
import zel.net.clients.ClientConnection
import zel.protocol.InboundFrame
import zel.protocol.typed.PacketDispatcher
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandlerAdapter
import io.netty.handler.codec.CorruptedFrameException
import io.netty.handler.codec.TooLongFrameException
import io.netty.util.ReferenceCountUtil
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.IOException
import java.util.concurrent.RejectedExecutionException

/** Hands each packet to the connection's lane. [shareBytes] caps one connection's in-flight bodies; see [ReadValve]. */
class PacketIntake(
    private val dispatcher: PacketDispatcher,
    private val pool: WorkerPool,
    share: Int,
    shareBytes: Long = WorkerSettings.defaultPerConnectionBytes(),
    lightBytes: Int = WireLimits().ordinaryFrameBytes,
) : ChannelInboundHandlerAdapter() {
    init {
        require(share > 0) { "a connection's share of the packet pool must be positive, got $share" }
        require(shareBytes > 0) { "a connection's byte share of the packet pool must be positive, got $shareBytes" }
    }

    private val lane = pool.executor()
    private val valve = ReadValve(share, pool.limited, shareBytes, lightBytes.toLong(), { pool.heavy }) { pool.full }

    override fun handlerAdded(ctx: ChannelHandlerContext) {
        ctx
            .channel()
            .attr(ClientConnection.KEY)
            .get()
            ?.runLogoutOn(lane)
    }

    override fun channelRead(
        ctx: ChannelHandlerContext,
        msg: Any,
    ) {
        if (msg !is InboundFrame) {
            ReferenceCountUtil.release(msg)
            ctx.close()
            return
        }
        val connection = ctx.channel().attr(ClientConnection.KEY).get()
        val bytes = msg.body.readableBytes()
        pool.acquire()
        pool.holdBytes(bytes)
        make(ctx, valve.handedOver(bytes))
        try {
            lane.execute {
                try {
                    deliver(connection, msg)
                } finally {
                    release(ctx, bytes)
                }
            }
        } catch (
            @Suppress("SwallowedException") stopping: RejectedExecutionException, // the server is stopping
        ) {
            msg.release()
            release(ctx, bytes)
        }
    }

    private fun release(
        ctx: ChannelHandlerContext,
        bytes: Int,
    ) {
        pool.freeBytes(bytes)
        pool.release()
        ctx.executor().execute { make(ctx, valve.cameBack(bytes)) }
    }

    override fun exceptionCaught(
        ctx: ChannelHandlerContext,
        cause: Throwable,
    ) {
        val connection = ctx.channel().attr(ClientConnection.KEY).get()
        if (cause is TooLongFrameException || cause is CorruptedFrameException) {
            connection?.warnings?.invalid("frame", "bad frame length")
        } else if (cause !is IOException) {
            log.warn("closing {} after an error", ctx.channel().remoteAddress(), cause)
        }
        ctx.close()
    }

    private fun make(
        ctx: ChannelHandlerContext,
        move: ReadValve.Move,
    ) {
        when (move) {
            ReadValve.Move.NOTHING -> Unit
            ReadValve.Move.SHUT -> ctx.channel().config().isAutoRead = false
            ReadValve.Move.OPEN -> ctx.channel().config().isAutoRead = true
            ReadValve.Move.WAIT_FOR_ROOM -> pool.whenRoom { ctx.executor().execute { make(ctx, valve.poolHasRoom()) } }
        }
    }

    // A failing handler loses only its packet.
    @Suppress("TooGenericExceptionCaught")
    private fun deliver(
        connection: ClientConnection?,
        frame: InboundFrame,
    ) {
        try {
            dispatcher.handle(connection, frame)
        } catch (failure: Exception) {
            log.error("packet {} failed", frame.messageId, failure)
        } finally {
            frame.release()
        }
    }

    private companion object {
        val log: Logger = LoggerFactory.getLogger(PacketIntake::class.java)
    }
}
