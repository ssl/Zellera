package zel.room

import zel.room.event.RoomEvent

fun interface RoomListener {
    fun on(
        scope: RoomScope,
        event: RoomEvent,
    )
}
