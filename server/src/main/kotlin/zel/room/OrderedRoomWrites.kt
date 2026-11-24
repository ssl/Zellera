package zel.room

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.Duration
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeUnit.MILLISECONDS

/** [writeLatest] coalesces saves until [flush] or [holdMillis], never reordering. */
class OrderedRoomWrites(
    private val inOrder: (key: Any, job: Runnable) -> Unit,
    private val patience: Duration = Duration.ofSeconds(DEFAULT_PATIENCE_SECONDS),
    private val holdMillis: Long = 0,
    private val later: (Runnable) -> Unit = { job ->
        CompletableFuture.delayedExecutor(holdMillis, MILLISECONDS).execute(job)
    },
) : RoomWrites {
    private data class Lane(
        val roomId: Int,
    )

    /** Also the lock that keeps the hand-over in order. */
    private class Held {
        val jobs = LinkedHashMap<Any, Runnable>()
        var due = false
    }

    private val held = ConcurrentHashMap<Int, Held>()

    override fun write(
        roomId: Int,
        job: Runnable,
    ) {
        val waiting = held[roomId] ?: return inOrder(Lane(roomId), job)
        synchronized(waiting) {
            handOver(roomId, waiting)
            inOrder(Lane(roomId), job)
        }
    }

    /** A held job under the same [key] gives way to [job]. */
    fun writeLatest(
        roomId: Int,
        key: Any,
        job: Runnable,
    ) {
        val waiting = held.computeIfAbsent(roomId) { Held() }
        val schedule =
            synchronized(waiting) {
                waiting.jobs.remove(key)
                waiting.jobs[key] = job
                !waiting.due.also { waiting.due = true }
            }
        if (schedule && holdMillis > 0) later(Runnable { flush(roomId) })
    }

    fun flush(roomId: Int) {
        val waiting = held[roomId] ?: return
        synchronized(waiting) { handOver(roomId, waiting) }
    }

    fun flushAll() = held.keys.forEach(::flush)

    private fun handOver(
        roomId: Int,
        waiting: Held,
    ) {
        waiting.due = false
        if (waiting.jobs.isEmpty()) return
        val jobs = waiting.jobs.values.toList()
        waiting.jobs.clear()
        inOrder(Lane(roomId), Runnable { jobs.forEach(Runnable::run) })
    }

    /** Lets a load see the unload's saves; false on timeout. */
    fun settle(roomId: Int): Boolean {
        val done = CountDownLatch(1)
        write(roomId, Runnable { done.countDown() })
        val settled =
            try {
                done.await(patience.toMillis(), TimeUnit.MILLISECONDS)
            } catch (_: InterruptedException) {
                Thread.currentThread().interrupt()
                false
            }
        if (!settled) log.warn("room {}: loading before its earlier writes finished", roomId)
        return settled
    }

    private companion object {
        val log: Logger = LoggerFactory.getLogger(OrderedRoomWrites::class.java)
        const val DEFAULT_PATIENCE_SECONDS = 10L
    }
}
