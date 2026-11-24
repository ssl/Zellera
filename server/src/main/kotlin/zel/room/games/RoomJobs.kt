package zel.room.games

import zel.room.Room
import zel.room.RoomScope

fun interface RoomJobs {
    fun after(
        room: Room,
        millis: Long,
        job: (RoomScope) -> Unit,
    )
}
