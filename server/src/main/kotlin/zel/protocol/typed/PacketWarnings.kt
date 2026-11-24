package zel.protocol.typed

import org.slf4j.Logger
import org.slf4j.LoggerFactory

class PacketWarnings(
    private val player: () -> String?,
    private val address: () -> String,
    private val now: () -> Long = System::currentTimeMillis,
) {
    @Volatile
    var current: IncomingPacket = IncomingPacket.NONE

    private var windowStart = Long.MIN_VALUE
    private var logged = 0
    private var dropped = 0

    fun invalid(
        packet: String,
        reason: String,
    ) = note(Kind.INVALID, packet, reason)

    fun refused(reason: String) = note(Kind.SUSPICIOUS, current.name, reason)

    fun unknownId(
        packet: IncomingPacket,
        reason: String,
    ) = note(Kind.UNKNOWN_ID, packet.name, reason)

    fun failed(cause: Exception) {
        val reason = if (current.secret) cause.javaClass.name else cause.toString()
        note(Kind.ERROR, current.name, reason.take(MAX_REASON), cause.takeUnless { current.secret })
    }

    private fun note(
        kind: Kind,
        packet: String,
        reason: String,
        cause: Exception? = null,
    ) {
        val droppedBefore = admit() ?: return
        val tail = if (droppedBefore > 0) " ($droppedBefore more not logged)" else ""
        val sender = player() ?: "no player (${address()})"
        // one finished line, so the client-controlled parts are never read as placeholders
        val line = "${kind.label} packet: $sender, $packet, $reason$tail"
        when (kind) {
            Kind.UNKNOWN_ID -> LOG.info(line)
            Kind.INVALID, Kind.SUSPICIOUS -> LOG.warn(line)
            Kind.ERROR -> if (cause == null) LOG.error(line) else LOG.error(line, cause)
        }
    }

    @Synchronized
    private fun admit(): Int? {
        val time = now()
        if (windowStart == Long.MIN_VALUE || time - windowStart >= WINDOW_MS) {
            windowStart = time
            logged = 0
        }
        if (logged >= LIMIT) {
            dropped++
            return null
        }
        logged++
        return dropped.also { dropped = 0 }
    }

    private enum class Kind(
        val label: String,
    ) {
        INVALID("invalid"),
        SUSPICIOUS("suspicious"),
        UNKNOWN_ID("unknown_id"),
        ERROR("error"),
    }

    private companion object {
        val LOG: Logger = LoggerFactory.getLogger(PacketWarnings::class.java)
        const val LIMIT = 10
        const val WINDOW_MS = 60_000L
        const val MAX_REASON = 255
    }
}
