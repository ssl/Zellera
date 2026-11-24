package zel.room

import zel.room.tick.TickPart
import java.util.concurrent.ConcurrentHashMap

/**
 * Writes each loaded room's held state every [intervalMillis], at the room's own offset; 0 leaves the writes to unload
 * and shutdown. A checkpoint only copies under the room lock what changed and hands it to the room's write lane: it
 * runs none of the unload's settling, so running wired, game clocks and passing furni states go on as they were.
 */
class RoomCheckpoints(
    private val intervalMillis: Long,
    private val save: (Room) -> Unit,
) : RoomPart {
    /** The next checkpoint of each loaded room; an entry is owned by its room's lock. */
    private val due = ConcurrentHashMap<Room, Long>()

    val tick: TickPart =
        TickPart { scope ->
            if (intervalMillis <= 0) return@TickPart
            val room = scope.room
            val now = scope.now()
            val next = due.getOrPut(room) { next(room.info.id, now) }
            if (now < next) return@TickPart
            save(room)
            due[room] = next(room.info.id, now)
        }

    /** The last part to unload: it writes what the parts before it left changed or held. */
    override fun unload(scope: RoomScope) {
        due.remove(scope.room)
        save(scope.room)
    }

    fun next(
        roomId: Int,
        now: Long,
    ): Long {
        val offset = Math.floorMod(roomId.toLong() * SPREAD, intervalMillis)
        return now - Math.floorMod(now - offset, intervalMillis) + intervalMillis
    }

    companion object {
        const val INTERVAL_KEY = "room.checkpoint.interval.minutes"
        const val DEFAULT_INTERVAL_MINUTES = 5

        /** Knuth's multiplicative hash: neighbouring room ids land far apart within the interval. */
        private const val SPREAD = 2_654_435_761L
    }
}
