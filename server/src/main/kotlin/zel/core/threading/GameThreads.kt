package zel.core.threading

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import zel.db.DatabaseWriter
import java.time.Duration
import java.util.concurrent.Delayed
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

class GameThreads(
    size: Int,
    private val writer: DatabaseWriter? = null,
) {
    data class Stopped(
        val clean: Boolean,
        val letGo: Int,
    )

    init {
        require(size > 0) { "the game pool needs at least one thread" }
    }

    private val workerNumbers = AtomicInteger()
    private val pool =
        ScheduledThreadPoolExecutor(size) { work -> Thread(work, "game-worker-${workerNumbers.incrementAndGet()}") }
            .apply {
                removeOnCancelPolicy = true
                executeExistingDelayedTasksAfterShutdownPolicy = false
            }

    @Volatile
    private var accepting = true

    /** An overload, not a default argument, so `threads.run { }` never resolves to the stdlib scope function. */
    fun run(task: Runnable): ScheduledFuture<*>? = run(task, 0L)

    /** Null once stopping began; work due now then runs on the caller, later work is dropped. */
    fun run(
        task: Runnable,
        afterMillis: Long,
    ): ScheduledFuture<*>? {
        if (accepting) {
            try {
                return pool.schedule(Caught(task), afterMillis, TimeUnit.MILLISECONDS)
            } catch (_: RejectedExecutionException) {
                // the pool closed between the check and the hand-over: handled below
            }
        }
        if (afterMillis <= 0L) {
            Caught(task).run()
        } else {
            LOG.debug("stopping: {} in {} ms is let go", task.javaClass.name, afterMillis)
        }
        return null
    }

    fun write(task: Runnable) {
        if (writer == null) run(task) else writer.write(WRITE_LABEL, task)
    }

    /** Without a writer it runs on the pool, unordered. */
    fun write(
        key: Any,
        task: Runnable,
    ) {
        if (writer == null) run(task) else writer.write(key, WRITE_LABEL, task)
    }

    fun awaitWrites(patience: Duration): Boolean = writer?.drain(patience) ?: true

    fun writePressure(): DatabaseWriter.Pressure? = writer?.pressure()

    fun queuedTasks(): Int = pool.queue.size

    /** How long the task that is due first has waited for a worker; zero when none is due. */
    fun lateness(): Duration {
        val untilDue = (pool.queue.peek() as? Delayed)?.getDelay(TimeUnit.NANOSECONDS) ?: return Duration.ZERO
        return Duration.ofNanos((-untilDue).coerceAtLeast(0))
    }

    /** What is queued still runs. */
    fun stopAccepting() {
        accepting = false
    }

    /** Drops delayed tasks, waits [patience] for the rest, then drops the queue and interrupts. */
    fun stop(patience: Duration = DEFAULT_PATIENCE): Stopped {
        stopAccepting()
        pool.shutdown()
        val emptied =
            try {
                pool.awaitTermination(patience.toNanos(), TimeUnit.NANOSECONDS)
            } catch (_: InterruptedException) {
                Thread.currentThread().interrupt()
                false
            }
        return if (emptied) Stopped(clean = true, letGo = 0) else Stopped(clean = false, pool.shutdownNow().size)
    }

    private class Caught(
        private val task: Runnable,
    ) : Runnable {
        @Suppress("TooGenericExceptionCaught") // a worker survives any task
        override fun run() {
            try {
                task.run()
            } catch (broken: Throwable) {
                LOG.error("a scheduled task failed", broken)
            }
        }
    }

    private companion object {
        val LOG: Logger = LoggerFactory.getLogger(GameThreads::class.java)
        val DEFAULT_PATIENCE: Duration = Duration.ofSeconds(10)
        const val WRITE_LABEL = "scheduler"
    }
}
