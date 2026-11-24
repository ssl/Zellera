package zel.net.server

import zel.core.load.ServerGauges
import io.netty.util.concurrent.DefaultEventExecutorGroup
import io.netty.util.concurrent.DefaultThreadFactory
import io.netty.util.concurrent.EventExecutor
import io.netty.util.concurrent.EventExecutorGroup
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

/** [backlog] caps waiting plus running packets, [backlogBytes] their bodies; an unlimited pool only counts. */
class WorkerPool(
    name: String,
    val threads: Int,
    val backlog: Int,
    val limited: Boolean,
    val backlogBytes: Long = Long.MAX_VALUE,
) : AutoCloseable {
    init {
        require(threads > 0 && backlog > 0) { "$name needs positive threads and backlog, got $threads and $backlog" }
        require(backlogBytes > 0) { "$name needs a positive byte backlog, got $backlogBytes" }
    }

    val executors: EventExecutorGroup = DefaultEventExecutorGroup(threads, DefaultThreadFactory(name))

    private val used = AtomicInteger()
    private val usedBytes = AtomicLong()
    private val waiting = ConcurrentLinkedQueue<Runnable>()

    val full: Boolean get() = limited && used.get() >= backlog

    /** Holds [backlogBytes] or more of packet bodies. */
    val heavy: Boolean get() = limited && usedBytes.get() >= backlogBytes

    fun heldBytes(): Long = usedBytes.get()

    fun occupancy() = ServerGauges.Occupancy(used.get(), backlog)

    fun executor(): EventExecutor = executors.next()

    fun acquire() {
        used.incrementAndGet()
    }

    fun release() {
        used.decrementAndGet()
        runWaiting()
    }

    fun holdBytes(bytes: Int) {
        usedBytes.addAndGet(bytes.toLong())
    }

    fun freeBytes(bytes: Int) {
        usedBytes.addAndGet(-bytes.toLong())
        runWaiting()
    }

    fun whenRoom(callback: Runnable) {
        waiting += callback
        runWaiting()
    }

    override fun close() {
        executors.shutdownGracefully(0, STOP_SECONDS, TimeUnit.SECONDS).syncUninterruptibly()
    }

    // One callback at a time, so a callback that fills the pool again holds the rest back.
    private fun runWaiting() {
        while (!full && !heavy) {
            (waiting.poll() ?: return).run()
        }
    }

    private companion object {
        const val STOP_SECONDS = 5L
    }
}
