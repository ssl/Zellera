package zel.game

import java.util.concurrent.atomic.AtomicLong

class LatencyTracker {
    private val nextAnswerAt = AtomicLong(Long.MIN_VALUE)

    fun mayAnswerRequest(
        nowMillis: Long,
        gapMillis: Long,
    ): Boolean {
        while (true) {
            val due = nextAnswerAt.get()
            if (nowMillis < due) return false
            if (nextAnswerAt.compareAndSet(due, nowMillis + gapMillis)) return true
        }
    }
}
