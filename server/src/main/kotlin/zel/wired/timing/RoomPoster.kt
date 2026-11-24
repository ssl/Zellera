package zel.wired.timing

import zel.room.RoomScope

/** Runs work on a room's worker under its lock, after a delay; false when it cannot be queued. */
fun interface RoomPoster {
    fun post(
        roomId: Int,
        delayMillis: Long,
        block: (RoomScope) -> Unit,
    ): Boolean

    companion object {
        val None: RoomPoster = RoomPoster { _, _, _ -> false }
    }
}
