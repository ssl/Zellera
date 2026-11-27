package zel.wired.store

import zel.room.Room
import zel.room.RoomPart
import zel.room.RoomScope
import zel.room.furni.RoomFurni
import zel.room.furni.StoredValues
import zel.room.map.Point
import java.util.concurrent.ConcurrentHashMap

class ReloadPoints(
    private val boxes: () -> RoomBoxForms? = { null },
) : RoomPart {
    private data class FurniPoint(
        val anchor: Point?,
        val direction: Int,
        val z: Double,
        val state: String,
        val wallPosition: String?,
    )

    private class RoomPoint(
        val furni: Map<Int, FurniPoint>,
        val boxes: Map<Int, StoredBox>,
    )

    private val points = ConcurrentHashMap<Int, RoomPoint>()

    private val reloading = ConcurrentHashMap.newKeySet<Int>()

    fun reloading(roomId: Int) {
        reloading += roomId
    }

    override fun unload(scope: RoomScope) {
        val roomId = scope.room.info.id
        if (!reloading.remove(roomId)) points.remove(roomId)
    }

    fun keep(room: Room) {
        val furni =
            room.furni.all().associate { it.id to FurniPoint(it.anchor, it.direction, it.z, it.state, it.wallPosition) }
        points[room.info.id] = RoomPoint(furni, boxes()?.kept(room).orEmpty())
    }

    fun restore(room: Room) {
        val point = points[room.info.id] ?: return
        point.furni.forEach { (id, kept) -> room.furni[id]?.let { restore(room, it, kept) } }
        boxes()?.restore(room, point.boxes)
    }

    private fun restore(
        room: Room,
        furni: RoomFurni,
        point: FurniPoint,
    ) {
        point.anchor?.let { room.furni.move(furni, it, point.direction) }
        furni.z = point.z
        if (!StoredValues.holds(furni)) furni.state = point.state
        furni.wallPosition = point.wallPosition
        furni.dirty = true
    }
}
