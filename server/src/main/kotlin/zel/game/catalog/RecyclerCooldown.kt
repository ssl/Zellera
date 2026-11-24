package zel.game.catalog

import zel.util.MILLIS_PER_SECOND
import java.util.concurrent.ConcurrentHashMap
import java.util.function.LongSupplier

class RecyclerCooldown(
    private val clock: LongSupplier,
) {
    private val lastRun = ConcurrentHashMap<Int, Long>()

    fun millisLeft(userId: Int): Long {
        val at = lastRun[userId] ?: return 0
        val left = at + TIMEOUT_SECONDS * MILLIS_PER_SECOND - clock.asLong
        if (left > 0) return left
        lastRun.remove(userId, at)
        return 0
    }

    fun waiting(userId: Int): Boolean = millisLeft(userId) > 0

    fun status(userId: Int): IntArray {
        val left = millisLeft(userId)
        return if (left >
            0
        ) {
            intArrayOf(STATUS_TIMEOUT, (left / MILLIS_PER_SECOND).toInt())
        } else {
            intArrayOf(STATUS_READY, 0)
        }
    }

    fun ran(userId: Int) {
        lastRun[userId] = clock.asLong
    }

    companion object {
        const val POINTS_COST = 10
        const val TIMEOUT_SECONDS = 10
        const val STATUS_READY = 1
        const val STATUS_TIMEOUT = 3
    }
}
