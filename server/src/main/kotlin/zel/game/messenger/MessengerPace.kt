package zel.game.messenger

/** Four messages in a row, then one more per 800 ms; a message over the pace is dropped silently. */
class MessengerPace {
    private var clearAt = Long.MIN_VALUE

    @Synchronized
    fun take(millis: Long): Boolean {
        if (clearAt != Long.MIN_VALUE && millis < clearAt - (BURST - 1) * INTERVAL_MILLIS) return false
        clearAt = maxOf(clearAt, millis) + INTERVAL_MILLIS
        return true
    }

    companion object {
        const val BURST = 4
        const val INTERVAL_MILLIS = 800L
    }
}
