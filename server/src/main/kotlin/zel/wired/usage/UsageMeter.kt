package zel.wired.usage

import kotlin.math.ceil
import kotlin.math.pow

/**
 * One room's wired usage reading: a sum cut to [KEPT] of itself at every [WINDOW_MILLIS]
 * boundary of the room clock. A firing is charged [PER_FIRING] before it runs and refused when that would pass the
 * [limit]; each furni or user its boxes reached adds [PER_REACHED] after, while the sum is still below the limit.
 */
class UsageMeter(
    val limit: Double,
) {
    private var stored = 0.0
    private var window: Long? = null

    /** False, with nothing charged, when it would pass the limit. A called stack is charged nothing before. */
    fun charge(
        now: Long,
        called: Boolean = false,
    ): Boolean {
        decayTo(now)
        val cost = if (called) 0.0 else PER_FIRING
        if (stored + cost > limit) return false
        stored += cost
        return true
    }

    /** What a run reached, charged one by one until the sum reaches the limit; the rest go free. */
    fun chargeAfter(
        now: Long,
        reached: Int,
    ) {
        decayTo(now)
        if (reached <= 0 || stored >= limit) return
        val charged =
            if (stored + PER_REACHED * reached < limit) {
                reached
            } else {
                ceil((limit - stored) / PER_REACHED).toInt().coerceIn(1, reached)
            }
        stored += PER_REACHED * charged
    }

    /** The sum as the monitor shows it, never changed by reading: up to one reached charge past the limit. */
    fun reading(now: Long): Double {
        val at = window ?: return 0.0
        return stored * kept(windowOf(now) - at)
    }

    private fun decayTo(now: Long) {
        val current = windowOf(now)
        val at = window
        if (at != null && current > at) stored *= kept(current - at)
        if (at == null || current > at) window = current
    }

    private fun windowOf(now: Long): Long = Math.floorDiv(now, WINDOW_MILLIS)

    private fun kept(windows: Long): Double = if (windows <= 0) 1.0 else KEPT.pow(windows.toDouble())

    companion object {
        const val PER_FIRING = 0.052
        const val PER_REACHED = 0.4
        const val WINDOW_MILLIS = 500L
        const val KEPT = 0.6
    }
}
