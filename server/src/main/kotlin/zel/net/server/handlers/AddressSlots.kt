package zel.net.server.handlers

import zel.net.server.proxy.IpLiteral
import io.netty.channel.Channel
import io.netty.util.AttributeKey
import java.util.concurrent.ConcurrentHashMap

/**
 * Sockets one subscriber line may hold open at a time; 0 is no cap. Loopback is uncounted: local tools and a proxy
 * on this machine use it.
 */
class AddressSlots(
    private val perAddress: Int,
) {
    private val held = ConcurrentHashMap<String, Int>()

    /** True when the channel holds a slot, now or from before; the slot goes back when the channel closes. */
    fun hold(
        channel: Channel,
        address: String?,
    ): Boolean {
        if (address == null || channel.hasAttr(SLOT)) return true
        if (!take(address)) return false
        channel.attr(SLOT).set(address)
        channel.closeFuture().addListener { release(address) }
        return true
    }

    fun take(address: String): Boolean {
        if (!counts(address)) return true
        var taken = false
        held.compute(IpLiteral.subscriber(address)) { _, count ->
            val now = count ?: 0
            taken = now < perAddress
            if (taken) now + 1 else now
        }
        return taken
    }

    fun release(address: String) {
        if (!counts(address)) return
        held.computeIfPresent(IpLiteral.subscriber(address)) { _, count -> (count - 1).takeIf { it > 0 } }
    }

    private fun counts(address: String): Boolean = perAddress > 0 && !IpLiteral.isLoopback(address)

    private companion object {
        val SLOT: AttributeKey<String> = AttributeKey.valueOf("address-slot")
    }
}
