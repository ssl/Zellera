package zel.db

import zel.core.Configuration

/**
 * A running write holds a database connection, so the writers are sized from the pool: a third of it, which leaves
 * the larger part to logins and packet handlers that read, and two at least, so one slow write never holds up every
 * other key. With the queue full a caller waits.
 */
data class WriterSize(
    val threads: Int,
    val capacity: Int,
) {
    init {
        require(threads >= 1) { "the database writer needs a thread, not $threads" }
    }

    companion object {
        const val THREADS_KEY = "database.writer.threads"
        const val LEAST_THREADS = 2
        private const val POOL_SHARE = 3
        const val QUEUE_KEY = "database.writer.queue.per_thread"
        const val QUEUE_PER_THREAD = 5_000

        fun forPool(
            connections: Int,
            queuePerThread: Int = QUEUE_PER_THREAD,
        ): WriterSize = of((connections / POOL_SHARE).coerceAtLeast(LEAST_THREADS), queuePerThread)

        fun of(
            threads: Int,
            queuePerThread: Int = QUEUE_PER_THREAD,
        ): WriterSize = WriterSize(threads, threads * queuePerThread)

        /** More writers than the pool has connections would only wait on it: a set count stops at the pool's size. */
        fun configured(configuration: Configuration): WriterSize {
            val connections =
                configuration.getInt("database.pool.max_connections", Database.DEFAULT_CONNECTIONS).coerceAtLeast(1)
            val set = configuration.getInt(THREADS_KEY, 0)
            val queue = configuration.getInt(QUEUE_KEY, QUEUE_PER_THREAD).takeIf { it > 0 } ?: QUEUE_PER_THREAD
            return if (set > 0) of(set.coerceAtMost(connections), queue) else forPool(connections, queue)
        }
    }
}
