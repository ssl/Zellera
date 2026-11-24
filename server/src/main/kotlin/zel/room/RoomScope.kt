package zel.room

import zel.room.event.RoomEvent
import zel.room.tick.RoomOutput

interface RoomScope {
    val room: Room
    val out: RoomOutput

    fun now(): Long

    fun raise(event: RoomEvent)
}
