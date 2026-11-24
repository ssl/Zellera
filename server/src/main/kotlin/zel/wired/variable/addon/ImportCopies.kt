package zel.wired.variable.addon

import com.google.gson.JsonObject
import zel.room.Room
import zel.room.furni.RoomFurni
import java.util.IdentityHashMap
import java.util.concurrent.ConcurrentHashMap

/** A box picked up and placed again starts empty. Guarded by the room's lock. */
class ImportCopies(
    private val keys: (Room, RoomFurni) -> JsonObject?,
) {
    private val rooms = ConcurrentHashMap<Room, IdentityHashMap<RoomFurni, ImportedAddons>>()

    fun of(
        room: Room,
        box: RoomFurni,
    ): ImportedAddons =
        rooms.getOrPut(room) { IdentityHashMap() }.getOrPut(box) { ImportedAddons.read(keys(room, box)) }

    fun replaced(
        room: Room,
        box: RoomFurni,
        addons: ImportedAddons,
    ) {
        rooms.getOrPut(room) { IdentityHashMap() }[box] = addons
    }

    fun forget(room: Room) {
        rooms.remove(room)
    }
}
