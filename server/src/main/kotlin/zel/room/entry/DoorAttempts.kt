package zel.room.entry

import java.util.concurrent.ConcurrentHashMap

/** Wrong door passwords per player and room: the first few are free, each one after that waits twice as long. */
class DoorAttempts(
    private val now: () -> Long = System::currentTimeMillis,
) {
    private data class Key(
        val playerId: Int,
        val roomId: Int,
    )

    private data class Tries(
        val failures: Int,
        val lastAt: Long,
        val lockedUntil: Long,
    )

    private val tries = ConcurrentHashMap<Key, Tries>()

    fun locked(
        playerId: Int,
        roomId: Int,
    ): Boolean = (tries[Key(playerId, roomId)]?.lockedUntil ?: 0) > now()

    fun failed(
        playerId: Int,
        roomId: Int,
    ) {
        val at = now()
        if (tries.size >= PRUNE_ABOVE) tries.values.removeIf { at - it.lastAt >= FORGET_MILLIS }
        tries.compute(Key(playerId, roomId)) { _, before ->
            val failures = if (before == null || at - before.lastAt >= FORGET_MILLIS) 1 else before.failures + 1
            Tries(failures, at, at + waitAfter(failures))
        }
    }

    fun passed(
        playerId: Int,
        roomId: Int,
    ) {
        tries.remove(Key(playerId, roomId))
    }

    companion object {
        const val FREE_TRIES = 3
        const val FIRST_WAIT_MILLIS = 30_000L
        const val LONGEST_WAIT_MILLIS = 900_000L
        const val FORGET_MILLIS = 1_800_000L

        private const val PRUNE_ABOVE = 10_000
        private const val MAX_DOUBLINGS = 10

        fun waitAfter(failures: Int): Long {
            if (failures < FREE_TRIES) return 0
            val doublings = (failures - FREE_TRIES).coerceAtMost(MAX_DOUBLINGS)
            return (FIRST_WAIT_MILLIS shl doublings).coerceAtMost(LONGEST_WAIT_MILLIS)
        }
    }
}
