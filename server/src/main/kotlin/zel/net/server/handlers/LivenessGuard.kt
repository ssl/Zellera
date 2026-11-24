package zel.net.server.handlers

import zel.net.clients.ClientConnection
import zel.protocol.InboundFrame
import zel.protocol.generated.PingOut
import zel.protocol.generated.PingOutCodec
import zel.protocol.generated.PongInCodec
import zel.protocol.outgoing.handshake.DisconnectReason
import zel.protocol.typed.ClientPacket
import zel.util.monotonicMillis
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandlerAdapter
import java.util.concurrent.TimeUnit

class LivenessGuard(
    pingAfterSeconds: Int,
    dropAfterSeconds: Int,
    loginSeconds: Int,
    private val wallClock: () -> Long = System::currentTimeMillis,
    private val clock: () -> Long = MONOTONIC_MILLIS,
) : ChannelInboundHandlerAdapter() {
    private val gapMillis = TimeUnit.SECONDS.toMillis(pingAfterSeconds.toLong())
    private val quietMillis = TimeUnit.SECONDS.toMillis(dropAfterSeconds.toLong())
    private val loginMillis = TimeUnit.SECONDS.toMillis(loginSeconds.toLong())
    private var watch: QuietWatch? = null

    override fun handlerAdded(ctx: ChannelHandlerContext) {
        if (ctx.channel().isActive) begin(ctx)
    }

    override fun channelActive(ctx: ChannelHandlerContext) {
        begin(ctx)
        ctx.fireChannelActive()
    }

    override fun channelRead(
        ctx: ChannelHandlerContext,
        msg: Any,
    ) {
        if (msg is InboundFrame) watch?.heard(clock(), pong = msg.isPong())
        ctx.fireChannelRead(msg)
    }

    private fun InboundFrame.isPong(): Boolean = this is ClientPacket && name == PongInCodec.name

    private fun begin(ctx: ChannelHandlerContext) {
        if (watch != null) return
        watch = QuietWatch(clock(), gapMillis, quietMillis, loginMillis)
        bookLook(ctx)
    }

    private fun bookLook(ctx: ChannelHandlerContext) {
        ctx.executor().schedule({ look(ctx) }, TickGrid.untilNext(wallClock()), TimeUnit.MILLISECONDS)
    }

    private fun look(ctx: ChannelHandlerContext) {
        if (!ctx.channel().isActive) return
        val connection = ctx.channel().attr(ClientConnection.KEY).get()
        when (watch?.due(clock(), withPlayer = connection?.player != null)) {
            QuietWatch.Call.PING -> {
                connection?.send(PingOutCodec, PingOut)
            }

            QuietWatch.Call.DROP_UNANSWERED -> {
                connection?.send(DisconnectReason(DisconnectReason.PONG_TIMEOUT))
                ctx.close()
            }

            QuietWatch.Call.DROP_QUIET, QuietWatch.Call.DROP_NOT_LOGGED_IN -> {
                ctx.close()
            }

            QuietWatch.Call.NOTHING, null -> {}
        }
        if (ctx.channel().isActive) bookLook(ctx)
    }

    companion object {
        val MONOTONIC_MILLIS: () -> Long = ::monotonicMillis
    }
}
