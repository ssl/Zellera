package zel.wired.addon.label

import zel.game.items.Item
import zel.room.Room
import zel.room.RoomPart
import zel.room.RoomScope
import zel.room.furni.place.Interactions
import zel.wired.store.WiredWriter
import java.util.concurrent.ConcurrentHashMap

class RoomLabels(
    private val store: LabelStore,
    private val writer: WiredWriter,
) : RoomPart {
    private val rooms = ConcurrentHashMap<Room, MutableMap<Int, Label>>()

    override fun load(room: Room) {
        val ids =
            room.furni
                .all()
                .filter { isLabel(it.definition) }
                .map { it.id }
        rooms[room] = store.load(ids).toMutableMap()
    }

    override fun unload(scope: RoomScope) {
        rooms.remove(scope.room)
    }

    fun of(
        room: Room,
        id: Int,
    ): Label = rooms[room]?.get(id) ?: Label()

    fun all(room: Room): List<Pair<Int, Label>> =
        room.furni
            .all()
            .filter { isLabel(it.definition) }
            .sortedBy { it.id }
            .map { it.id to of(room, it.id) }

    fun set(
        room: Room,
        id: Int,
        label: Label,
    ) {
        rooms.computeIfAbsent(room) { HashMap() }[id] = label
        writer.write(room.info.id, Runnable { store.save(id, label) })
    }

    fun placed(
        room: Room,
        id: Int,
    ) {
        rooms[room]?.remove(id)
    }

    fun pickedUp(
        room: Room,
        id: Int,
    ) {
        rooms[room]?.remove(id)
        writer.write(room.info.id, Runnable { store.delete(id) })
    }

    private fun isLabel(definition: Item) = Interactions.of(definition) == Label.CODE
}
