package zel.net.server.handlers

import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandlerAdapter
import io.netty.handler.codec.http.FullHttpRequest
import io.netty.handler.codec.http.HttpHeaderNames
import io.netty.handler.codec.http.HttpHeaderValues
import zel.net.server.proxy.ClientAddress
import zel.net.server.proxy.ProxyTrust
import zel.wired.variable.addon.VariablesBridge
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

class PreUpgradeGuard(
    private val trust: ProxyTrust,
    private val cap: PreUpgradeCap,
    private val timeoutSeconds: Int,
) : ChannelInboundHandlerAdapter() {
    private var peer: String? = null
    private var timeout: ScheduledFuture<*>? = null
    private val released = AtomicBoolean()

    override fun handlerAdded(ctx: ChannelHandlerContext) {
        begin(ctx)
    }

    override fun channelActive(ctx: ChannelHandlerContext) {
        begin(ctx)
        ctx.fireChannelActive()
    }

    override fun channelRead(
        ctx: ChannelHandlerContext,
        msg: Any,
    ) {
        if (!released.get() && msg is FullHttpRequest && (msg.isUpgrade() || msg.isBridge())) release()
        ctx.fireChannelRead(msg)
    }

    override fun channelInactive(ctx: ChannelHandlerContext) {
        release()
        ctx.fireChannelInactive()
    }

    private fun begin(ctx: ChannelHandlerContext) {
        if (peer != null || released.get()) return
        val address = ClientAddress.peer(ctx.channel()) ?: return
        if (!trust.forwards(address)) {
            released.set(true)
            return
        }
        if (!cap.enter(address)) {
            released.set(true)
            ctx.close()
            return
        }
        peer = address
        timeout = ctx.executor().schedule({ ctx.close() }, timeoutSeconds.toLong(), TimeUnit.SECONDS)
    }

    private fun release() {
        if (!released.compareAndSet(false, true)) return
        peer?.let(cap::leave)
        timeout?.cancel(false)
    }

    private fun FullHttpRequest.isUpgrade(): Boolean =
        headers().containsValue(HttpHeaderNames.UPGRADE, HttpHeaderValues.WEBSOCKET, true)

    private fun FullHttpRequest.isBridge(): Boolean = uri().substringBefore('?') == VariablesBridge.PATH
}
