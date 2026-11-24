package zel.net.server.proxy

import io.netty.channel.Channel
import io.netty.util.AttributeKey
import java.net.InetSocketAddress

object ClientAddress {
    /** Set at the upgrade when a trusted proxy named the player's address. */
    val FORWARDED: AttributeKey<String> = AttributeKey.valueOf("forwarded-address")

    private const val GONE = "closed"

    fun peer(channel: Channel): String? = (channel.remoteAddress() as? InetSocketAddress)?.address?.hostAddress

    fun of(channel: Channel): String = channel.attr(FORWARDED)?.get() ?: peer(channel) ?: GONE
}
