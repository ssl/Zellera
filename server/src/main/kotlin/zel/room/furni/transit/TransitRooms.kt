package zel.room.furni.transit

import zel.room.Room
import java.util.concurrent.ConcurrentHashMap

/** Each loaded room's [RoomTransit]; an entry is read and written only under its room's lock. */
class TransitRooms {
    private val rooms = ConcurrentHashMap<Room, RoomTransit>()

    fun of(room: Room): RoomTransit = rooms.getOrPut(room) { RoomTransit() }

    fun add(room: Room) {
        rooms[room] = RoomTransit()
    }

    fun remove(room: Room): RoomTransit? = rooms.remove(room)
}
