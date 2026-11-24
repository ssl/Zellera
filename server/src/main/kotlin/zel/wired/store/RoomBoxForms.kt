package zel.wired.store

import com.google.gson.JsonObject
import zel.room.Room
import zel.room.RoomPart
import zel.room.RoomScope
import zel.room.furni.RoomFurni
import zel.room.furni.place.Interactions
import zel.wired.BoxSettingsSource
import zel.wired.box.BoxSettings
import zel.wired.form.DialogSpecs
import zel.wired.form.SaveChecks
import zel.wired.form.WiredMasks
import java.util.concurrent.ConcurrentHashMap

class RoomBoxForms(
    private val store: WiredStore,
    private val dialogs: DialogSpecs,
    private val writer: WiredWriter,
    /** Rows other rooms read (a variable another room may share) are written at once. */
    private val readElsewhere: (RoomFurni) -> Boolean = { false },
) : BoxSettingsSource,
    RoomPart {
    private class Forms(
        val roomId: Int,
        val boxes: HashMap<Int, StoredBox>,
        var masks: WiredMasks,
    ) {
        val known = HashMap<Int, RoomFurni>()
    }

    private val rooms = ConcurrentHashMap<Room, Forms>()

    /** The room logs outlive the room's unload; they go with a server restart. */
    private val logs = ConcurrentHashMap<Int, RoomLog>()

    override fun load(room: Room) {
        rooms[room] = read(room)
    }

    override fun unload(scope: RoomScope) {
        rooms.remove(scope.room)
    }

    override fun settings(
        room: Room,
        box: RoomFurni,
    ): BoxSettings? {
        val spec = dialogs[Interactions.of(box.definition)] ?: return null
        val forms = forms(room)
        if (forms.known[box.id] !== box) placed(forms, box, spec.form.fresh)
        val stored = forms.boxes[box.id] ?: return null
        return if (stored.isFresh) spec.form.fresh else SaveChecks.bounded(spec.form, stored.settings)
    }

    fun save(
        room: Room,
        box: RoomFurni,
        settings: BoxSettings,
    ) {
        settings(room, box)
        val forms = forms(room)
        forms.boxes[box.id] = StoredBox(settings, false, forms.boxes[box.id]?.extras)
        held(forms, box)
    }

    fun extras(
        room: Room,
        box: RoomFurni,
    ): JsonObject? {
        settings(room, box)
        return forms(room).boxes[box.id]?.extras
    }

    fun updateExtras(
        room: Room,
        box: RoomFurni,
        change: (JsonObject) -> Unit,
    ) {
        val fresh = settings(room, box)
        val forms = forms(room)
        val known = forms.boxes[box.id]
        val extras = known?.extras?.deepCopy() ?: JsonObject()
        change(extras)
        val updated = (known ?: StoredBox(fresh ?: BoxSettings.EMPTY, true)).copy(extras = extras)
        forms.boxes[box.id] = updated
        held(forms, box)
    }

    private fun placed(
        forms: Forms,
        box: RoomFurni,
        fresh: BoxSettings,
    ) {
        forms.known[box.id] = box
        val row = StoredBox(fresh, true)
        forms.boxes[box.id] = row
        held(forms, box)
    }

    /** Held until the room's next hand-over (checkpoint, unload, stop, a write); newest wins. */
    private fun held(
        forms: Forms,
        box: RoomFurni,
    ) {
        val id = box.id
        val row = forms.boxes[id] ?: return
        val save = Runnable { store.saveBox(id, row) }
        if (readElsewhere(box)) writer.write(forms.roomId, save) else writer.writeLatest(forms.roomId, BoxRow(id), save)
    }

    private data class BoxRow(
        val furniId: Int,
    )

    fun masks(room: Room): WiredMasks = forms(room).masks

    fun log(room: Room): RoomLog = logs.computeIfAbsent(room.info.id, ::RoomLog)

    fun saveMasks(
        room: Room,
        masks: WiredMasks,
    ) {
        forms(room).masks = masks
        val roomId = room.info.id
        writer.write(roomId, Runnable { store.saveMasks(roomId, masks) })
    }

    private fun forms(room: Room): Forms = rooms.getOrPut(room) { read(room) }

    private fun read(room: Room): Forms =
        Forms(
            room.info.id,
            HashMap(store.roomBoxes(room.info.id)),
            store.masks(room.info.id) ?: WiredMasks.FRESH,
        ).also { forms ->
            room.furni.all().forEach { forms.known[it.id] = it }
        }
}
