package zel.room.rights

import zel.room.Room
import zel.room.RoomPart
import zel.room.RoomScope
import java.util.concurrent.ConcurrentHashMap

class RoomMutes(
    private val store: RightsStore,
    private val nowSeconds: () -> Int,
) : RoomPart {
    /** Each room's entry is read and written only under that room's lock. */
    private val rooms = ConcurrentHashMap<Room, Muted>()

    override fun load(room: Room) {
        rooms[room] = Muted(store.mutes(room.info.id, nowSeconds()).toMutableMap())
    }

    override fun unload(scope: RoomScope) {
        rooms.remove(scope.room)
    }

    fun of(room: Room): Muted = rooms.getOrPut(room) { Muted(HashMap()) }

    /** One room's mutes: player id to the mute's end (unix seconds), and the whole-room mute. */
    class Muted(
        val players: MutableMap<Int, Int>,
        var all: Boolean = false,
    )
}
