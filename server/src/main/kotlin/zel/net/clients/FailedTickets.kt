package zel.net.clients

import org.slf4j.LoggerFactory
import zel.core.Configuration
import zel.net.server.proxy.IpLiteral
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/**
 * Login tickets that failed, per subscriber line. Each failure counts for the window divided by the limit, so a
 * line that used its limit gets one more try at that pace; a turned-away login is not counted again. Loopback is
 * uncounted, as for the socket cap.
 */
class FailedTickets(
    private val limit: () -> Int,
    private val windowSeconds: () -> Int,
    private val nanoTime: () -> Long = System::nanoTime,
) {
    constructor(configuration: Configuration) : this(
        { configuration.getInt(LIMIT_KEY, DEFAULT_LIMIT) },
        { configuration.getInt(WINDOW_KEY, DEFAULT_WINDOW_SECONDS) },
    )

    private class Tally(
        val failures: Double,
        val at: Long,
    )

    private val tallies = ConcurrentHashMap<String, Tally>()

    fun refuses(address: String): Boolean {
        val most = limit()
        if (most <= 0 || IpLiteral.isLoopback(address)) return false
        val tally = tallies[IpLiteral.subscriber(address)] ?: return false
        return standing(tally, nanoTime(), most) > most - 1
    }

    fun record(address: String) {
        val most = limit()
        if (most <= 0 || IpLiteral.isLoopback(address)) return
        val now = nanoTime()
        val line = IpLiteral.subscriber(address)
        if (!tallies.containsKey(line) && !hasRoom(now, most)) return
        tallies.compute(line) { _, known -> Tally((known?.let { standing(it, now, most) } ?: 0.0) + 1, now) }
    }

    private fun standing(
        tally: Tally,
        now: Long,
        most: Int,
    ): Double {
        val forgiven = (now - tally.at).toDouble() * most / TimeUnit.SECONDS.toNanos(windowSeconds().toLong())
        return (tally.failures - forgiven).coerceAtLeast(0.0)
    }

    /** Past the cap the forgiven lines go; if none went, new lines stay uncounted rather than grow the map. */
    private fun hasRoom(
        now: Long,
        most: Int,
    ): Boolean {
        if (tallies.size < MAX_LINES) return true
        tallies.values.removeIf { standing(it, now, most) <= 0.0 }
        if (tallies.size < MAX_LINES) return true
        log.warn("failed logins come from over {} addresses at once; further ones are not counted", MAX_LINES)
        return false
    }

    companion object {
        const val LIMIT_KEY = "login.ticket.failures.per_address"
        const val WINDOW_KEY = "login.ticket.failures.window.seconds"
        const val DEFAULT_LIMIT = 30
        const val DEFAULT_WINDOW_SECONDS = 120
        private const val MAX_LINES = 65_536
        private val log = LoggerFactory.getLogger(FailedTickets::class.java)
    }
}
