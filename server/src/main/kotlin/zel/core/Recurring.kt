package zel.core

import zel.core.threading.GameThreads
import java.time.Duration
import java.util.concurrent.ScheduledFuture

/** Repeating work on a fixed grid from the first round; the period is re-read before each wait; no overlap. */
class Recurring(
    private val threading: GameThreads,
    private val period: () -> Duration,
    private val nanoClock: () -> Long = System::nanoTime,
    private val work: () -> Unit,
) {
    private var dueAt = 0L
    private var booked: ScheduledFuture<*>? = null
    private var ended = false

    @Synchronized
    fun start(first: Duration = Duration.ZERO) {
        dueAt = nanoClock() + first.toNanos()
        booked = threading.run(::round, first.toMillis())
    }

    /** A round that is running finishes; none follows. */
    @Synchronized
    fun end() {
        ended = true
        booked?.cancel(false)
        booked = null
    }

    private fun round() {
        try {
            if (!isEnded()) work()
        } finally {
            bookNext()
        }
    }

    @Synchronized
    private fun isEnded() = ended

    /** A round that is already late runs at once and the grid starts again from now. */
    @Synchronized
    private fun bookNext() {
        if (ended) return
        val now = nanoClock()
        dueAt = (dueAt + period().toNanos().coerceAtLeast(SHORTEST_NANOS)).coerceAtLeast(now)
        booked = threading.run(::round, Duration.ofNanos(dueAt - now).toMillis())
    }

    private companion object {
        /** A period of nothing must not spin a worker. */
        val SHORTEST_NANOS = Duration.ofSeconds(1).toNanos()
    }
}
