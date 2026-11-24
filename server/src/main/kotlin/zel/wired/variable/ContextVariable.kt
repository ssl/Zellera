package zel.wired.variable

import zel.wired.firing.HeldValue

class ContextVariable(
    override val entry: VariableEntry,
    override val boxId: Int,
    private val clock: () -> Long,
) : Variable {
    private val hasValue get() = entry.flags.hasValue

    override fun read(
        on: VariableOn,
        holder: Holder,
    ): HeldValue? = on.firing?.context?.get(id)

    override fun give(
        on: VariableOn,
        holder: Holder,
        value: Long,
        override: Boolean,
        origin: Int,
    ): Boolean {
        val context = on.firing?.context ?: return false
        if (!override && context.containsKey(id)) return false
        val now = clock()
        context[id] = HeldValue(if (hasValue) value else 0, now, now)
        VariableBlink.flash(on, boxId, VariableBlink.GIVEN)
        return true
    }

    override fun write(
        on: VariableOn,
        holder: Holder,
        value: Long,
        origin: Int,
    ): Boolean {
        val context = on.firing?.context ?: return false
        if (!hasValue) return false
        val old = context[id] ?: return false
        context[id] = old.copy(value = value, updated = clock())
        VariableBlink.flash(on, boxId, VariableBlink.moved(old.value, value), old.value, value)
        return true
    }

    override fun remove(
        on: VariableOn,
        holder: Holder,
        origin: Int,
    ): Boolean =
        (on.firing?.context?.remove(id) != null).also { removed ->
            if (removed) VariableBlink.flash(on, boxId, VariableBlink.REMOVED)
        }
}
