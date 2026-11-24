package zel.game.messenger

import java.util.Locale
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger

/** [id] is 24 hex digits (send seconds, a per-process part, a counter), unique and time-sorted across restarts. */
class Message(
    val fromId: Int,
    val toId: Int,
    val message: String,
    val timestamp: Int,
    val iconId: Int = 0,
    id: String? = null,
) {
    val id: String = id?.takeIf { it.isNotEmpty() } ?: nextId(timestamp)

    val isIcon: Boolean get() = iconId > 0

    companion object {
        private const val COUNTER_MASK = 0xFFFFFF
        private const val PROCESS_BITS = 40

        private val seed = UUID.randomUUID()
        private val processPart =
            "%010x".format(Locale.ROOT, seed.mostSignificantBits ushr (Long.SIZE_BITS - PROCESS_BITS))
        private val counter = AtomicInteger((seed.leastSignificantBits and COUNTER_MASK.toLong()).toInt())

        fun nextId(timestamp: Int): String {
            val count = counter.getAndIncrement() and COUNTER_MASK
            return "%08x%s%06x".format(Locale.ROOT, timestamp, processPart, count)
        }
    }
}
