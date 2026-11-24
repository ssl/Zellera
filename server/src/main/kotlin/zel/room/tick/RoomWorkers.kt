package zel.room.tick

import org.slf4j.LoggerFactory
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.TimeUnit

/** Single-threaded lanes; a room's lane follows from its id, so its tick and tasks never run at once. */
class RoomWorkers(
    val count: Int,
    private val tickMillis: Long = RoomTiming.TICK_MILLIS,
) {
    private val lanes: List<ScheduledThreadPoolExecutor>

    init {
        require(count > 0) { "room workers: $count" }
        lanes = List(count) { lane(it + 1) }
    }

    fun indexOf(roomId: Int): Int = Math.floorMod(roomId, count)

    fun scheduleTick(
        roomId: Int,
        tick: Runnable,
    ): ScheduledFuture<*> = laneOf(roomId).scheduleAtFixedRate(tick, tickMillis, tickMillis, TimeUnit.MILLISECONDS)

    /** Queues [task] on the room's lane; false once the lanes are stopped. */
    fun post(
        roomId: Int,
        delayMillis: Long = 0,
        task: Runnable,
    ): Boolean {
        val lane = laneOf(roomId)
        if (lane.isShutdown) return false
        return try {
            lane.schedule({ shielded(roomId, task) }, delayMillis, TimeUnit.MILLISECONDS)
            true
        } catch (_: RejectedExecutionException) {
            false
        }
    }

    fun shutdown() {
        lanes.forEach { it.shutdown() }
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(STOP_WAIT_SECONDS)
        val stuck =
            lanes.count { lane ->
                !lane.awaitTermination((deadline - System.nanoTime()).coerceAtLeast(0), TimeUnit.NANOSECONDS)
            }
        if (stuck == 0) return
        log.warn("{} room lanes still busy after {} s; interrupting them", stuck, STOP_WAIT_SECONDS)
        lanes.forEach { it.shutdownNow() }
    }

    private fun laneOf(roomId: Int): ScheduledThreadPoolExecutor = lanes[indexOf(roomId)]

    private fun lane(number: Int) =
        ScheduledThreadPoolExecutor(1) { Thread(it, "room-lane-$number").apply { isDaemon = false } }
            .apply { removeOnCancelPolicy = true }

    @Suppress("TooGenericExceptionCaught") // a failing task must not end the lane, and its error is logged
    private fun shielded(
        roomId: Int,
        task: Runnable,
    ) {
        try {
            task.run()
        } catch (e: Throwable) {
            log.error("room {}: posted task failed", roomId, e)
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(RoomWorkers::class.java)
        private const val STOP_WAIT_SECONDS = 5L

        private const val FEWEST_LANES = 2
        private const val MOST_LANES = 16

        const val WORKERS_KEY = "room.tick.lanes"

        fun count(
            configured: Int,
            processors: Int,
        ): Int = configured.takeIf { it > 0 } ?: processors.coerceIn(FEWEST_LANES, MOST_LANES)
    }
}
