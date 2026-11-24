package zel.wired.variable

import zel.wired.firing.HeldValue
import zel.wired.store.WiredWriter
import java.util.concurrent.ConcurrentHashMap

class ValueHome(
    val roomId: Int,
    val boxId: Int,
    val stored: Boolean,
    val values: MutableMap<Holder, HeldValue>,
    private val store: VariableStore,
    private val writer: WiredWriter,
) {
    /** [hasValue] false stores the flag without a value. */
    fun put(
        holder: Holder,
        held: HeldValue,
        hasValue: Boolean,
    ) {
        values[holder] = held
        if (stored) write(holder, held, hasValue)
    }

    /** Atomic; old and new value, or null. */
    fun update(
        holder: Holder,
        absent: HeldValue?,
        hasValue: Boolean,
        change: (HeldValue) -> HeldValue,
    ): Pair<HeldValue, HeldValue>? {
        var old: HeldValue? = null
        val new = values.compute(holder) { _, held -> (held ?: absent)?.also { old = it }?.let(change) }
        val before = old ?: return null
        if (stored && new != null) write(holder, new, hasValue)
        return new?.let { before to it }
    }

    fun drop(holder: Holder): HeldValue? {
        val gone = values.remove(holder)
        if (gone != null && stored) erase(holder)
        return gone
    }

    /** Values several rooms write at once; their saves store what the map holds when they run, so the last one wins. */
    private val shared = values is ConcurrentHashMap<*, *>

    private fun write(
        holder: Holder,
        held: HeldValue,
        hasValue: Boolean,
    ) {
        val saved = { if (shared) values[holder] else held }
        val save: (HeldValue) -> Unit =
            when (holder) {
                is Holder.Player -> { now -> store.saveUser(roomId, holder.id, boxId, now.shown(hasValue), now) }
                is Holder.Furni -> { now -> store.saveFurni(roomId, holder.id, boxId, now.shown(hasValue), now) }
                Holder.Room -> { now -> store.saveRoom(roomId, boxId, now) }
                is Holder.Bot, is Holder.Pet, Holder.Context -> return
            }
        writer.writeLatest(roomId, Saved(boxId, holder), Runnable { saved()?.let(save) })
    }

    private fun HeldValue.shown(hasValue: Boolean): Long? = value.takeIf { hasValue }

    private fun erase(holder: Holder) {
        val delete: () -> Unit =
            when (holder) {
                is Holder.Player -> { -> store.deleteUser(roomId, holder.id, boxId) }
                is Holder.Furni -> { -> store.deleteFurni(roomId, holder.id, boxId) }
                is Holder.Bot, is Holder.Pet, Holder.Room, Holder.Context -> return
            }
        writer.writeLatest(roomId, Saved(boxId, holder), Runnable { if (!shared || values[holder] == null) delete() })
    }

    /** The saves and deletes of one row in one tick write only the last. */
    private data class Saved(
        val boxId: Int,
        val holder: Holder,
    )
}
