package zel.game.bots

class BotWiredTalkLimit {
    private val recent = LongArray(BURST_LINES)
    private var count = 0
    private var next = 0
    private var quietUntil = 0L

    @Synchronized
    fun allow(now: Long): Boolean {
        if (now < quietUntil) return false
        recent[next] = now
        next = (next + 1) % BURST_LINES
        if (count < BURST_LINES) count++
        if (count == BURST_LINES && now - recent[next] <= BURST_WINDOW_MS) {
            quietUntil = now + QUIET_MS
            count = 0
        }
        return true
    }

    private companion object {
        const val BURST_LINES = 10
        const val BURST_WINDOW_MS = 1000L
        const val QUIET_MS = 5000L
    }
}
