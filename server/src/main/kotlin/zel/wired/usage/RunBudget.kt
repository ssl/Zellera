package zel.wired.usage

import kotlin.math.max
import kotlin.math.min

/**
 * A room's wired run time against its share of a processor: run time is owed and paid off at the share's rate.
 * Guarded by the room's lock; times are the room clock's milliseconds.
 */
class RunBudget(
    private val limits: () -> Limits = { Limits() },
) {
    data class Limits(
        val sharePercent: Int = 10,
        val burstMillis: Int = 500,
        val heavyAfterSeconds: Int = 10,
        val pauseAfterSeconds: Int = 0,
        val pauseSeconds: Int = 60,
    )

    var heavy = false
        private set

    private var owed = 0.0
    private var strained = 0.0
    private var at: Long? = null
    private var pausedUntil: Long? = null
    private val pauses = ArrayDeque<Long>()
    private val raised = ArrayList<WiredError>()

    fun isPaused(now: Long): Boolean = pausedUntil?.let { now < it } == true

    /** False while the room is paused or owes more run time than its burst. */
    fun open(now: Long): Boolean {
        advance(now)
        return pausedUntil == null && owed <= limits().burstMillis
    }

    fun spent(
        now: Long,
        millis: Double,
    ) {
        advance(now)
        if (pausedUntil != null) return
        val burst = limits().burstMillis
        val wasOver = owed > burst
        owed += millis
        if (!wasOver && owed > burst) raised += WiredError.EXECUTOR_OVERLOAD
    }

    /** What happened since the last call, each once. */
    fun takeRaised(): List<WiredError> = raised.distinct().also { raised.clear() }

    private fun advance(now: Long) {
        val last = at
        at = now
        val until = pausedUntil
        if (until != null) {
            if (now < until) return
            pausedUntil = null
            heavy = false
        }
        if (last == null || now <= last) return
        val limits = limits()
        val elapsed = (now - max(last, until ?: last)).toDouble()
        val share = limits.sharePercent.coerceIn(1, FULL) / FULL.toDouble()
        val over = owed - limits.burstMillis
        val busy = if (over > 0) min(elapsed, over / share) else 0.0
        owed = max(0.0, owed - elapsed * share)
        strained = max(0.0, strained + busy - (elapsed - busy) / CALM_PER_STRAINED)
        judge(now, limits)
    }

    private fun judge(
        now: Long,
        limits: Limits,
    ) {
        val pauseAfter = limits.pauseAfterSeconds * MILLIS
        when {
            pauseAfter > 0 && strained >= pauseAfter -> {
                pause(now, limits)
            }

            !heavy && strained >= limits.heavyAfterSeconds * MILLIS -> {
                heavy = true
                raised += WiredError.MARKED_AS_HEAVY
            }

            heavy && strained <= 0.0 -> {
                heavy = false
            }
        }
    }

    private fun pause(
        now: Long,
        limits: Limits,
    ) {
        while (pauses.isNotEmpty() && now - pauses.first() > REMEMBERED_MILLIS) pauses.removeFirst()
        val length = limits.pauseSeconds.coerceAtLeast(1) * MILLIS shl min(pauses.size, MAX_DOUBLINGS)
        pauses.addLast(now)
        pausedUntil = now + length
        owed = 0.0
        strained = 0.0
        heavy = true
        raised += WiredError.KILLED
    }

    private companion object {
        const val FULL = 100
        const val MILLIS = 1000L

        /** A strained second is forgiven by this many calm ones. */
        const val CALM_PER_STRAINED = 4.0
        const val REMEMBERED_MILLIS = 3_600_000L
        const val MAX_DOUBLINGS = 4
    }
}
