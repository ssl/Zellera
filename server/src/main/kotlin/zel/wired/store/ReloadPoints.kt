package zel.wired.store

import zel.room.Room
import zel.room.RoomPart
import zel.room.RoomScope
import zel.room.furni.RoomFurni
import zel.room.furni.StoredValues
import zel.room.map.Point
import java.util.concurrent.ConcurrentHashMap

/** A room's reload point; it lasts until the room unloads by itself, the reload's own unload keeps it. */
class ReloadPoints : RoomPart {
    private data class FurniPoint(
        val anchor: Point?,
        val direction: Int,
        val z: Double,
        val state: String,
        val wallPosition: String?,
    )

    private val points = ConcurrentHashMap<Int, Map<Int, FurniPoint>>()

    private val reloading = ConcurrentHashMap.newKeySet<Int>()

    fun reloading(roomId: Int) {
        reloading += roomId
    }

    override fun unload(scope: RoomScope) {
        val roomId = scope.room.info.id
        if (!reloading.remove(roomId)) points.remove(roomId)
    }

    fun keep(room: Room) {
        points[room.info.id] =
            room.furni.all().associate { it.id to FurniPoint(it.anchor, it.direction, it.z, it.state, it.wallPosition) }
    }

    fun restore(room: Room) {
        points[room.info.id]?.forEach { (id, point) -> room.furni[id]?.let { restore(room, it, point) } }
    }

    private fun restore(
        room: Room,
        furni: RoomFurni,
        point: FurniPoint,
    ) {
        point.anchor?.let { room.furni.move(furni, it, point.direction) }
        furni.z = point.z
        // Hits, engravings, rents and the like are saved data of the furni, not a state: a rollback leaves them.
        if (!StoredValues.holds(furni)) furni.state = point.state
        furni.wallPosition = point.wallPosition
        furni.dirty = true
    }
}
