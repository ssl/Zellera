package zel.game.catalog

import java.util.concurrent.ConcurrentHashMap

/**
 * Wrong codes are counted per player for as long as the server runs, so logging in again does not clear them.
 * Five tries per quarter of an hour leave a guesser a few hundred codes a day.
 */
class VoucherLockout(
    private val clock: () -> Int,
) {
    private class Tries(
        val since: Int,
        val wrong: Int,
        val lockedUntil: Int,
    )

    private val tries = ConcurrentHashMap<Int, Tries>()

    /** Seconds the player still has to wait, 0 when free to try. */
    fun secondsLeft(playerId: Int): Int = maxOf(0, (tries[playerId]?.lockedUntil ?: 0) - clock())

    fun wrongCode(playerId: Int) {
        val now = clock()
        tries.compute(playerId) { _, before ->
            val running = before?.takeIf { now - it.since < WINDOW_SECONDS }
            val wrong = (running?.wrong ?: 0) + 1
            if (wrong >= MAX_WRONG) Tries(now, 0, now + LOCK_SECONDS) else Tries(running?.since ?: now, wrong, 0)
        }
        if (tries.size > SWEEP_ABOVE) {
            tries.values.removeIf { now - it.since >= WINDOW_SECONDS && it.lockedUntil <= now }
        }
    }

    fun redeemed(playerId: Int) {
        tries.remove(playerId)
    }

    companion object {
        const val MAX_WRONG = 5
        const val WINDOW_SECONDS = 600
        const val LOCK_SECONDS = 900
        private const val SWEEP_ABOVE = 10_000
    }
}
