package zel.game.players

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.ArrayDeque

/** Ordered per player, parallel across players; a failed write is logged and the next still runs. */
internal class PlayerSaveQueue(
    private val executor: (Runnable) -> Unit,
) {
    private class Lane {
        val writes = ArrayDeque<() -> Unit>()
        val whenIdle = mutableListOf<() -> Unit>()
    }

    /** Guarded by `this`. */
    private val lanes = HashMap<Int, Lane>()

    fun submit(
        playerId: Int,
        write: () -> Unit,
    ) {
        val start =
            synchronized(this) {
                val existing = lanes[playerId]
                val lane = existing ?: Lane().also { lanes[playerId] = it }
                lane.writes.add(write)
                existing == null
            }
        if (start) dispatch(playerId)
    }

    @Synchronized
    fun isIdle(playerId: Int): Boolean = playerId !in lanes

    fun whenIdle(
        playerId: Int,
        action: () -> Unit,
    ) {
        val queued =
            synchronized(this) {
                lanes[playerId]?.whenIdle?.add(action) ?: false
            }
        if (!queued) action()
    }

    @Suppress("TooGenericExceptionCaught") // a pool that refuses work throws its own runtime exception
    private fun dispatch(playerId: Int) {
        try {
            executor(Runnable { drain(playerId) })
        } catch (refused: RuntimeException) {
            // The database threads take no more work (the server is stopping): write here rather than out of order.
            LOGGER.debug("Writes of player {} run on the caller", playerId, refused)
            drain(playerId)
        }
    }

    private fun drain(playerId: Int) {
        while (true) {
            var idle: List<() -> Unit> = emptyList()
            val write =
                synchronized(this) {
                    val lane = checkNotNull(lanes[playerId]) { "no lane for player $playerId" }
                    lane.writes.poll().also {
                        if (it == null) {
                            lanes.remove(playerId)
                            idle = lane.whenIdle
                        }
                    }
                }
            if (write == null) {
                idle.forEach { run(it, playerId) }
                return
            }
            run(write, playerId)
        }
    }

    @Suppress("TooGenericExceptionCaught") // one failed write must not stop the player's later writes
    private fun run(
        task: () -> Unit,
        playerId: Int,
    ) {
        try {
            task()
        } catch (exception: Exception) {
            LOGGER.error("A write of player {} failed", playerId, exception)
        }
    }

    private companion object {
        val LOGGER: Logger = LoggerFactory.getLogger(PlayerSaveQueue::class.java)
    }
}
