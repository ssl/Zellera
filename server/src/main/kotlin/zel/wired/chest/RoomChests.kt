package zel.wired.chest

import zel.protocol.generated.ObjectDataUpdateOut
import zel.protocol.generated.ObjectDataUpdateOutCodec
import zel.room.Room
import zel.room.RoomListener
import zel.room.RoomPart
import zel.room.RoomScope
import zel.room.event.RoomEvent
import zel.room.furni.RoomFurni
import zel.room.furni.place.FurniWire
import zel.room.furni.place.Interactions
import zel.room.tick.Audience
import zel.room.tick.Lane
import zel.room.unit.UnitKind
import zel.wired.store.WiredWriter
import java.util.concurrent.ConcurrentHashMap

/** [onLeft] runs first when a player leaves the room, before their chests close and auto-lock. */
class RoomChests(
    private val store: ChestStore,
    private val writer: WiredWriter,
    private val onLeft: (RoomScope, Int) -> Unit,
) : RoomPart,
    RoomListener {
    private class Looks {
        val viewers = HashMap<Int, MutableSet<Int>>()

        val shown = HashSet<Pair<Int, Int>>()

        val players = HashMap<Int, Int>()

        var lastFailureDialog = Long.MIN_VALUE
    }

    private val rooms = ConcurrentHashMap<Room, Looks>()

    /** Furni ids are unique over rooms; each chest is used under its room's lock. */
    private val byId = ConcurrentHashMap<Int, Chest>()

    private val leaving = ConcurrentHashMap<Int, Chest>()

    override fun load(room: Room) {
        rooms[room] = Looks()
        val rows = store.roomChests(room.info.id)
        room.furni.all().forEach { furni ->
            ChestType.of(Interactions.of(furni.definition))?.let { type ->
                byId[furni.id] = rows[furni.id]?.toChest(furni.id, type) ?: Chest(furni.id, type)
            }
        }
    }

    override fun unload(scope: RoomScope) {
        rooms.remove(scope.room)
        scope.room.furni
            .all()
            .forEach { byId.remove(it.id) }
    }

    override fun on(
        scope: RoomScope,
        event: RoomEvent,
    ) {
        val looks = looks(scope.room)
        when (event) {
            is RoomEvent.Entered -> {
                (scope.room.units[event.unitIndex]?.kind as? UnitKind.Player)?.let {
                    looks.players[event.unitIndex] =
                        it.playerId
                }
            }

            is RoomEvent.Left -> {
                looks.players.remove(event.unitIndex)?.let { left(scope, it) }
            }

            else -> {}
        }
    }

    private fun left(
        scope: RoomScope,
        playerId: Int,
    ) {
        onLeft(scope, playerId)
        val looks = looks(scope.room)
        looks.shown.removeAll { it.first == playerId }
        val closing =
            looks.viewers
                .filterValues { playerId in it }
                .keys
                .toList()
        closing.forEach { id ->
            looks.viewers[id]?.remove(playerId)
            scope.room.furni[id]?.let { furni -> chest(furni)?.let { showLid(scope, furni, it) } }
        }
        autoLock(scope, playerId)
    }

    private fun autoLock(
        scope: RoomScope,
        playerId: Int,
    ) {
        scope.room.furni
            .all()
            .filter { it.ownerId == playerId }
            .forEach { furni ->
                val chest = chest(furni)
                if (chest != null && chest.autoLock && !chest.locked) {
                    chest.locked = true
                    changed(scope, furni, chest)
                }
            }
    }

    fun chest(furni: RoomFurni): Chest? {
        val type = ChestType.of(Interactions.of(furni.definition)) ?: return null
        return byId.getOrPut(furni.id) {
            returning(furni.id) ?: store.chest(furni.id)?.toChest(furni.id, type) ?: Chest(furni.id, type)
        }
    }

    fun known(id: Int): Chest? = byId[id]

    fun inRoom(
        room: Room,
        id: Int,
    ): Pair<RoomFurni, Chest>? = room.furni[id]?.let { furni -> chest(furni)?.let { furni to it } }

    fun wiredOf(
        room: Room,
        ids: List<Int>,
    ): List<Pair<RoomFurni, Chest>> = ids.distinct().mapNotNull { inRoom(room, it) }.filter { it.second.wiredEnabled }

    fun all(room: Room): List<Pair<RoomFurni, Chest>> =
        room.furni.all().mapNotNull { furni -> chest(furni)?.let { furni to it } }

    fun addViewer(
        room: Room,
        chestId: Int,
        playerId: Int,
    ) {
        looks(room).viewers.getOrPut(chestId) { LinkedHashSet() }.add(playerId)
    }

    fun removeViewer(
        room: Room,
        chestId: Int,
        playerId: Int,
    ) {
        looks(room).viewers[chestId]?.remove(playerId)
    }

    fun hasViewers(
        room: Room,
        chestId: Int,
    ): Boolean = looks(room).viewers[chestId].orEmpty().isNotEmpty()

    fun markShown(
        room: Room,
        playerId: Int,
        chestId: Int,
    ) {
        looks(room).shown.add(playerId to chestId)
    }

    fun wasShown(
        room: Room,
        playerId: Int,
        chestId: Int,
    ): Boolean = playerId to chestId in looks(room).shown

    fun forget(
        room: Room,
        chestId: Int,
    ) {
        val looks = looks(room)
        looks.viewers.remove(chestId)
        looks.shown.removeAll { it.second == chestId }
        byId.remove(chestId)
    }

    fun takeFailureDialog(
        room: Room,
        now: Long,
    ): Boolean {
        val looks = looks(room)
        if (looks.lastFailureDialog != Long.MIN_VALUE &&
            now - looks.lastFailureDialog < FAILURE_DIALOG_GAP
        ) {
            return false
        }
        looks.lastFailureDialog = now
        return true
    }

    fun changed(
        scope: RoomScope,
        furni: RoomFurni,
        chest: Chest,
    ) {
        shown(scope, furni, chest)
        save(scope.room.info.id, chest)
    }

    /** Shows the chest's new state; the caller saves it. */
    fun shown(
        scope: RoomScope,
        furni: RoomFurni,
        chest: Chest,
    ) {
        setLid(scope.room, furni, chest)
        send(scope, furni)
    }

    fun showLid(
        scope: RoomScope,
        furni: RoomFurni,
        chest: Chest,
    ) {
        if (setLid(scope.room, furni, chest)) send(scope, furni)
    }

    fun setLid(
        room: Room,
        furni: RoomFurni,
        chest: Chest,
    ): Boolean {
        val state = chest.lid(hasViewers(room, chest.id))
        if (furni.state == state) return false
        furni.state = state
        furni.dirty = true
        return true
    }

    fun send(
        scope: RoomScope,
        furni: RoomFurni,
    ) = scope.out.send(
        Lane.Now,
        Audience.Room,
        ObjectDataUpdateOutCodec,
        ObjectDataUpdateOut(furni.id.toString(), FurniWire.stuff(furni)),
    )

    fun save(
        roomId: Int,
        chest: Chest,
    ) {
        val id = chest.id
        val record = ChestRecord.of(chest)
        writer.write(roomId, Runnable { store.saveChest(id, record) })
    }

    /** On that room's lane; a room placing it meanwhile takes it, skipping the save. */
    fun saveLeaving(
        roomId: Int,
        chest: Chest,
    ) {
        val id = chest.id
        val record = ChestRecord.of(chest)
        leaving[id] = chest
        writer.write(
            roomId,
            Runnable {
                synchronized(chest) {
                    if (leaving[id] === chest) {
                        store.saveChest(id, record)
                        leaving.remove(id, chest)
                    }
                }
            },
        )
    }

    private fun returning(id: Int): Chest? =
        leaving[id]?.let { chest -> synchronized(chest) { chest.takeIf { leaving.remove(id, it) } } }

    private fun looks(room: Room): Looks = rooms.computeIfAbsent(room) { Looks() }

    private companion object {
        /** Shown as "once per 20 s"; the gap is a little under that so a retry right at 20 s goes through. */
        const val FAILURE_DIALOG_GAP = 19_100L
    }
}
