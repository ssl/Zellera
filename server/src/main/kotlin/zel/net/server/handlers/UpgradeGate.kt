package zel.net.server.handlers

import zel.core.Configuration
import zel.net.server.HttpRefusal
import zel.net.server.proxy.ClientAddress
import zel.net.server.proxy.ProxyTrust
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandlerAdapter
import io.netty.handler.codec.http.FullHttpRequest
import io.netty.handler.codec.http.HttpHeaderNames
import io.netty.handler.codec.http.HttpHeaderValues
import io.netty.handler.codec.http.HttpHeaders

/**
 * A direct peer took its slot when it connected; a player behind a proxy takes one here, under the address the
 * proxy named, and none when it named none.
 */
class UpgradeGate(
    configuration: Configuration,
    private val trust: ProxyTrust,
    private val slots: AddressSlots = AddressSlots(0),
) : ChannelInboundHandlerAdapter() {
    private val origins = AllowedOrigins(configuration.getValue("network.origin.hosts", ""))
    private val addressHeader: String? =
        configuration.getValue("network.client_address.header", DEFAULT_ADDRESS_HEADER).trim().ifEmpty { null }

    override fun channelRead(
        ctx: ChannelHandlerContext,
        msg: Any,
    ) {
        if (msg !is FullHttpRequest || !msg.isUpgrade()) {
            ctx.fireChannelRead(msg)
            return
        }
        val refusal = refusalOf(ctx, msg.headers())
        if (refusal == null) {
            ctx.fireChannelRead(msg)
        } else {
            msg.release()
            refusal.close(ctx)
        }
    }

    private fun refusalOf(
        ctx: ChannelHandlerContext,
        headers: HttpHeaders,
    ): HttpRefusal? {
        if (!origins.admits(headers.get(HttpHeaderNames.ORIGIN))) return HttpRefusal.FORBIDDEN
        val channel = ctx.channel()
        val peer = ClientAddress.peer(channel)
        val proxied = trust.forwards(peer)
        val forwarded = forwardedAddress(peer, proxied, headers)
        forwarded?.let { channel.attr(ClientAddress.FORWARDED).set(it) }
        return if (slots.hold(channel, if (proxied) forwarded else peer)) null else HttpRefusal.TOO_MANY
    }

    private fun FullHttpRequest.isUpgrade(): Boolean =
        headers().get(HttpHeaderNames.UPGRADE)?.let(HttpHeaderValues.WEBSOCKET::contentEqualsIgnoreCase) == true

    private fun forwardedAddress(
        peer: String?,
        proxied: Boolean,
        headers: HttpHeaders,
    ): String? {
        val name = (if (trust.isEdge(peer)) EDGE_ADDRESS_HEADER else addressHeader) ?: return null
        val sent = headers.get(name) ?: return null
        if (proxied) return trust.playerIn(sent)
        trust.headerIgnored(name, peer)
        return null
    }

    private companion object {
        const val DEFAULT_ADDRESS_HEADER = "X-Forwarded-For"

        /** The edge network's own header, which it sets itself whatever the visitor sent. */
        const val EDGE_ADDRESS_HEADER = "CF-Connecting-IP"
    }
}
