package zel.net.server.proxy

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import zel.core.Configuration
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

/** Whose word about a player's address counts: the listed proxies and, when switched on, the edge network. */
class ProxyTrust(
    private val configuration: Configuration?,
    private val edge: CloudflareEdge,
    private val nanoTime: () -> Long = System::nanoTime,
) {
    private class Listed(
        val setting: String,
        val blocks: List<AddressBlock>,
    )

    @Volatile
    private var listed = Listed("", emptyList())

    private val ignored = AtomicInteger()
    private val reportedAt = AtomicLong(Long.MIN_VALUE)

    fun isEdge(peer: String?): Boolean = edge.contains(peer)

    fun forwards(peer: String?): Boolean {
        val address = IpLiteral.bytes(peer) ?: return false
        return edge.contains(peer) || proxies().any { it.contains(address) }
    }

    /**
     * The player's address in a forwarded list: the last entry that is not itself one of our proxies, since
     * everything left of it is whatever the sender wrote. Null when that entry is no address.
     */
    fun playerIn(forwardedList: String): String? {
        val hops = forwardedList.split(',').map(String::trim).filter(String::isNotEmpty)
        val player = hops.lastOrNull { !forwards(it) } ?: hops.firstOrNull()
        return player?.takeIf { IpLiteral.bytes(it) != null }
    }

    /** One line a minute at most, however many peers send the header. */
    fun headerIgnored(
        header: String,
        peer: String?,
    ) {
        ignored.incrementAndGet()
        val now = nanoTime()
        val last = reportedAt.get()
        if (last != Long.MIN_VALUE && now - last < REPORT_GAP_NANOS) return
        if (!reportedAt.compareAndSet(last, now)) return
        log.warn(
            "{} ignored {} times, last from {}: only the proxies in {} are believed (addresses or CIDR blocks)",
            header,
            ignored.getAndSet(0),
            peer,
            PROXIES_KEY,
        )
    }

    private fun proxies(): List<AddressBlock> {
        val setting = configuration?.getValue(PROXIES_KEY, "").orEmpty()
        val known = listed
        if (known.setting == setting) return known.blocks
        val entries = setting.split(',').map(String::trim).filter(String::isNotEmpty)
        val blocks = entries.mapNotNull { entry -> AddressBlock.parse(entry) ?: null.also { unreadable(entry) } }
        listed = Listed(setting, blocks)
        return blocks
    }

    private fun unreadable(entry: String) =
        log.warn("{}: '{}' is neither an address nor a CIDR block and trusts nothing", PROXIES_KEY, entry)

    private companion object {
        val log: Logger = LoggerFactory.getLogger(ProxyTrust::class.java)
        const val PROXIES_KEY = "network.client_address.trusted_proxies"
        val REPORT_GAP_NANOS = TimeUnit.MINUTES.toNanos(1)
    }
}
