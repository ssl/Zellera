package zel.wired.score

import java.util.concurrent.ConcurrentHashMap

/** Keeps each variable's holder rows for [ttlMillis], so repeated or paged subscribes share one read. */
class RecentHolders(
    private val next: LeaderboardStore,
    private val ttlMillis: Long = DEFAULT_TTL_MILLIS,
    private val clock: () -> Long = System::currentTimeMillis,
) : LeaderboardStore by next {
    private class Read(
        val at: Long,
        val holders: List<LeaderHolder>,
    )

    private val reads = ConcurrentHashMap<Pair<Int, Int>, Read>()

    override fun holders(
        roomId: Int,
        boxId: Int,
    ): List<LeaderHolder> {
        val now = clock()
        val key = roomId to boxId
        reads[key]?.takeIf { now - it.at < ttlMillis }?.let { return it.holders }
        reads.values.removeIf { now - it.at >= ttlMillis }
        return next.holders(roomId, boxId).also { reads[key] = Read(now, it) }
    }

    override fun changed() = reads.clear()

    private companion object {
        const val DEFAULT_TTL_MILLIS = 1_000L
    }
}
