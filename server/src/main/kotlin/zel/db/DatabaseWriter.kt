package zel.db

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executor
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.Semaphore
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit.NANOSECONDS
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.LongAdder

/** Writes under one key run in order, others in parallel. At most [capacity] wait; callers block beyond that. */
class DatabaseWriter(
    threads: Int,
    val capacity: Int,
) : Executor {
    data class Pressure(
        val waiting: Int,
        val capacity: Int,
        val failed: Long,
    )

    private inner class Write(
        val label: String,
        val job: Runnable,
    ) : Runnable {
        val done = CountDownLatch(1)

        // Even an Error never kills the thread: a lost writer leaves the queue to fill until every caller blocks.
        @Suppress("TooGenericExceptionCaught")
        override fun run() {
            running.incrementAndGet()
            try {
                job.run()
            } catch (failed: Throwable) {
                failures.increment()
                log.error("database write {} failed", label, failed)
            } finally {
                running.decrementAndGet()
                finish()
            }
        }

        fun finish() {
            pending.remove(this)
            done.countDown()
        }
    }

    private class WriterThread(
        task: Runnable,
        name: String,
    ) : Thread(task, name)

    private inner class Lane(
        private val key: Any,
        private var current: Write?,
    ) : Runnable {
        override fun run() {
            while (true) {
                current?.run() ?: return
                lanes.compute(key) { _, queued ->
                    queued?.removeFirst()
                    current = queued?.firstOrNull()
                    queued?.ifEmpty { null }
                }
            }
        }
    }

    private val queue = LinkedBlockingQueue<Runnable>()
    private val places = Semaphore(capacity)
    private val lanes = ConcurrentHashMap<Any, ArrayDeque<Write>>()
    private val pending: MutableSet<Write> = ConcurrentHashMap.newKeySet()
    private val running = AtomicInteger()
    private val failures = LongAdder()
    private val threadNumbers = AtomicInteger()
    private val workers: ThreadPoolExecutor

    init {
        require(threads > 0) { "a database writer needs at least one thread" }
        require(capacity > 0) { "a database writer needs at least one place in its queue" }
        workers =
            ThreadPoolExecutor(
                threads,
                threads,
                0L,
                NANOSECONDS,
                queue,
            ) { task -> WriterThread(task, "$THREAD_PREFIX${threadNumbers.incrementAndGet()}") }
        workers.prestartAllCoreThreads()
    }

    override fun execute(task: Runnable) = write(PLAIN_LABEL, task)

    /** Refused once closed or when the caller is interrupted waiting for room. */
    fun write(
        label: String,
        job: Runnable,
    ) {
        val write = Write(label, job)
        pending.add(write)
        try {
            handOver(write)
        } catch (refused: RejectedExecutionException) {
            write.finish()
            throw refused
        }
    }

    fun write(
        key: Any,
        label: String,
        job: Runnable,
    ) {
        if (workers.isShutdown) throw closed()
        val write = Write(label, job)
        pending.add(write)
        var first = false
        lanes.compute(key) { _, queued -> (queued ?: ArrayDeque<Write>().also { first = true }).apply { add(write) } }
        if (!first) return
        try {
            handOver(Lane(key, write))
        } catch (refused: RejectedExecutionException) {
            lanes.remove(key)?.forEach(Write::finish)
            throw refused
        }
    }

    fun pressure(): Pressure = Pressure(capacity - places.availablePermits(), capacity, failures.sum())

    fun drain(patience: Duration): Boolean {
        val deadline = System.nanoTime() + patience.toNanos()
        // ArrayList copies through toArray, which tolerates writes finishing during the copy (toList does not)
        return ArrayList(pending).all { it.done.await((deadline - System.nanoTime()).coerceAtLeast(0), NANOSECONDS) }
    }

    /** Gives the threads [patience], then runs the rest itself; false if a thread stayed busy. */
    fun close(patience: Duration = Duration.ofSeconds(DEFAULT_PATIENCE_SECONDS)): Boolean {
        workers.shutdown()
        val finished =
            try {
                workers.awaitTermination(patience.toNanos(), NANOSECONDS)
            } catch (_: InterruptedException) {
                Thread.currentThread().interrupt()
                false
            }
        ArrayList<Runnable>().also(queue::drainTo).forEach(Runnable::run)
        if (!finished) log.error("database writers still busy after {}: {} running", patience, running.get())
        return finished
    }

    /** A writer thread with no free place runs the work itself, since waiting would deadlock on its own kind. */
    private fun handOver(work: Runnable) {
        if (workers.isShutdown) throw closed()
        if (!places.tryAcquire()) {
            if (Thread.currentThread() is WriterThread) return work.run()
            waitForPlace()
        }
        try {
            workers.execute {
                places.release()
                work.run()
            }
        } catch (_: RejectedExecutionException) {
            places.release()
            throw closed()
        }
    }

    private fun waitForPlace() {
        try {
            places.acquire()
        } catch (interrupted: InterruptedException) {
            Thread.currentThread().interrupt()
            throw RejectedExecutionException("interrupted while waiting for a place in the queue", interrupted)
        }
    }

    private fun closed() = RejectedExecutionException("the database writer is closed")

    private companion object {
        val log: Logger = LoggerFactory.getLogger(DatabaseWriter::class.java)
        const val THREAD_PREFIX = "db-writer-"
        const val PLAIN_LABEL = "db.write"
        const val DEFAULT_PATIENCE_SECONDS = 30L
    }
}
