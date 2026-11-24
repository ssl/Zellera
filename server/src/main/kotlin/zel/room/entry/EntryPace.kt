package zel.room.entry

import java.util.concurrent.ConcurrentHashMap

/** Room opens per player: a few at once, then one every [REFILL_MILLIS]. */
class EntryPace(
    private val now: () -> Long = System::currentTimeMillis,
) {
    private data class Bucket(
        val left: Int,
        val since: Long,
    )

    private val buckets = ConcurrentHashMap<Int, Bucket>()

    fun accept(playerId: Int): Boolean {
        val at = now()
        if (buckets.size >= PRUNE_ABOVE) buckets.values.removeIf { at - it.since >= BURST * REFILL_MILLIS }
        var accepted = false
        buckets.compute(playerId) { _, before ->
            val refilled = refill(before ?: Bucket(BURST, at), at)
            accepted = refilled.left > 0
            if (accepted) refilled.copy(left = refilled.left - 1) else refilled
        }
        return accepted
    }

    private fun refill(
        bucket: Bucket,
        at: Long,
    ): Bucket {
        val gained = ((at - bucket.since) / REFILL_MILLIS).toInt().coerceAtLeast(0)
        if (gained == 0) return bucket
        val left = (bucket.left + gained).coerceAtMost(BURST)
        return Bucket(left, if (left == BURST) at else bucket.since + gained * REFILL_MILLIS)
    }

    companion object {
        const val BURST = 5
        const val REFILL_MILLIS = 2_000L

        private const val PRUNE_ABOVE = 10_000
    }
}
