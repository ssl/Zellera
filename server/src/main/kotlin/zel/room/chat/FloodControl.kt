package zel.room.chat

import zel.util.MILLIS_PER_SECOND
import java.util.concurrent.ConcurrentHashMap

class FloodControl {
    private val windows = ConcurrentHashMap<Int, Window>()
    private val mutedUntil = ConcurrentHashMap<Int, Long>()

    fun mutedFor(
        playerId: Int,
        now: Long,
    ): Long {
        val left = (mutedUntil[playerId] ?: return 0) - now
        if (left <= 0) {
            mutedUntil.remove(playerId)
            return 0
        }
        return left
    }

    /** Counts a line; true when the window was already full, which starts a mute and empties it. */
    fun trips(
        playerId: Int,
        login: Any,
        rule: FloodRule,
        now: Long,
        muteSeconds: Int = MUTE_SECONDS,
    ): Boolean {
        val window = windowOf(playerId, login) ?: Window(login).also { windows[playerId] = it }
        val full = window.count(now, rule)
        if (full) mutedUntil[playerId] = now + muteSeconds * MILLIS_PER_SECOND
        return full
    }

    /** The count belongs to one login and would keep its player alive. */
    fun forget(playerId: Int) {
        windows.remove(playerId)
    }

    private fun windowOf(
        playerId: Int,
        login: Any,
    ): Window? = windows[playerId]?.takeIf { it.login == login }

    private class Window(
        val login: Any,
    ) {
        private val stamps = ArrayDeque<Long>()

        fun count(
            now: Long,
            rule: FloodRule,
        ): Boolean {
            stamps.removeAll { now - it >= rule.windowMillis }
            val full = stamps.size >= rule.lines
            if (full) {
                stamps.clear()
            } else {
                stamps.addLast(now)
            }
            return full
        }
    }

    companion object {
        const val MUTE_SECONDS = 30
    }
}
