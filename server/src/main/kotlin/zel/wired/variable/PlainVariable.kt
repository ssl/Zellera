package zel.wired.variable

import zel.wired.firing.HeldValue

class PlainVariable(
    override val entry: VariableEntry,
    override val boxId: Int,
    private val home: ValueHome,
    private val changes: VariableChanges,
    private val clock: () -> Long,
) : Variable {
    private val hasValue get() = entry.flags.hasValue
    private val isGlobal get() = entry.target == VariableTarget.GLOBAL

    override fun read(
        on: VariableOn,
        holder: Holder,
    ): HeldValue? = home.values[holder] ?: absent(holder)

    override fun give(
        on: VariableOn,
        holder: Holder,
        value: Long,
        override: Boolean,
        origin: Int,
    ): Boolean {
        if (isGlobal || (!override && home.values.containsKey(holder))) return false
        val now = clock()
        val kept = if (hasValue) value else 0
        home.put(holder, HeldValue(kept, now, now), hasValue)
        val shown = kept.takeIf { hasValue }
        changes.raise(on, VariableChange(id, boxId, holder, origin, ChangeType.CREATED, null, shown))
        return true
    }

    override fun write(
        on: VariableOn,
        holder: Holder,
        value: Long,
        origin: Int,
    ): Boolean {
        if (!entry.flags.canWriteValue) return false
        val old = read(on, holder) ?: return false
        home.put(holder, old.copy(value = value, updated = clock()), hasValue)
        changes.raise(on, VariableChange(id, boxId, holder, origin, ChangeType.VALUE_CHANGED, old.value, value))
        return true
    }

    override fun change(
        on: VariableOn,
        holder: Holder,
        origin: Int,
        next: (Long) -> Long,
    ): Boolean {
        if (!entry.flags.canWriteValue) return false
        val now = clock()
        val absent = absent(holder)
        val (old, new) =
            home.update(holder, absent, hasValue) { it.copy(value = next(it.value), updated = now) } ?: return false
        changes.raise(on, VariableChange(id, boxId, holder, origin, ChangeType.VALUE_CHANGED, old.value, new.value))
        return true
    }

    override fun remove(
        on: VariableOn,
        holder: Holder,
        origin: Int,
    ): Boolean {
        if (isGlobal) return false
        val old = home.drop(holder) ?: return false
        val shown = old.value.takeIf { hasValue }
        changes.raise(on, VariableChange(id, boxId, holder, origin, ChangeType.DELETED, shown, null))
        return true
    }

    override fun holders(on: VariableOn): List<Holder> = Presence.present(on, home.values.keys)

    /** A room variable always has a value for the room; a user or furni has none of it. */
    private fun absent(holder: Holder): HeldValue? = ZERO.takeIf { isGlobal && holder == Holder.Room }

    private companion object {
        val ZERO = HeldValue(0, 0, 0)
    }
}
