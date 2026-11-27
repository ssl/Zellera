package zel.wired.effect.user

import kotlin.math.min

/**
 * How many copies Place furni may make now: each one is a database transaction under the room's lock, so a room
 * gets a group's worth at once and a few a second after that. Guarded by the room's lock.
 */
class CopyAllowance {
    private var left = BURST.toDouble()
    private var at: Long? = null

    fun take(now: Long): Boolean {
        val last = at
        if (last != null && now > last) left = min(BURST.toDouble(), left + (now - last) * PER_MILLI)
        at = now
        if (left < 1.0) return false
        left -= 1.0
        return true
    }

    companion object {
        /** The furni one box picks at most, so one firing can copy a whole picked group. */
        const val BURST = 20
        const val PER_SECOND = 5

        /** Copies a room holds at once; Remove furni makes room again. */
        const val MAX_LIVE = 200
        const val MAX_LIVE_KEY = "wired.place_furni.max_copies"
        private const val PER_MILLI = PER_SECOND / 1000.0
    }
}
