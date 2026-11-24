package zel.room.chat

import zel.util.MILLIS_PER_SECOND
import java.util.concurrent.ConcurrentHashMap

class FloodControl {
    private val windows = ConcurrentHashMap<Int, Window>()

    fun mutedFor(
        playerId: Int,
        login: Any,
        now: Long,
    ): Long = windowOf(playerId, login)?.muteLeft(now) ?: 0

    /** Counts a line; true when the window was already full, which starts a mute and empties it. */
    fun trips(
        playerId: Int,
        login: Any,
        rule: FloodRule,
        now: Long,
        muteSeconds: Int = MUTE_SECONDS,
    ): Boolean {
        val window = windowOf(playerId, login) ?: Window(login).also { windows[playerId] = it }
        return window.count(now, rule, muteSeconds * MILLIS_PER_SECOND)
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
        private var quietUntil = 0L

        fun muteLeft(now: Long): Long = (quietUntil - now).coerceAtLeast(0)

        fun count(
            now: Long,
            rule: FloodRule,
            muteMillis: Long,
        ): Boolean {
            stamps.removeAll { now - it >= rule.windowMillis }
            val full = stamps.size >= rule.lines
            if (full) {
                stamps.clear()
                quietUntil = now + muteMillis
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
