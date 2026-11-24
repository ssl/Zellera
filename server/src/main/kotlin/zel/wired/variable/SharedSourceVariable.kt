package zel.wired.variable

import zel.wired.firing.HeldValue

/**
 * A shared variable in its own room: every change of its value also reaches the loaded rooms that hold a reference to
 * it, as [ReferenceVariable] does the other way. On the reference's 'variable changed' triggers, a web API change
 * (room loaded or not) counts as external, a change by the room's own wired as another room; neither as in room.
 */
class SharedSourceVariable(
    private val inner: Variable,
    private val roomId: Int,
    private val variables: RoomVariables,
) : Variable by inner {
    override fun give(
        on: VariableOn,
        holder: Holder,
        value: Long,
        override: Boolean,
        origin: Int,
    ): Boolean =
        inner.give(on, holder, value, override, origin).also { done ->
            if (done) reach(holder, origin, ChangeType.CREATED, null, shown(on, holder))
        }

    override fun write(
        on: VariableOn,
        holder: Holder,
        value: Long,
        origin: Int,
    ): Boolean {
        val old = inner.read(on, holder)?.value
        return inner.write(on, holder, value, origin).also { done ->
            if (done) reach(holder, origin, ChangeType.VALUE_CHANGED, old, value)
        }
    }

    override fun change(
        on: VariableOn,
        holder: Holder,
        origin: Int,
        next: (Long) -> Long,
    ): Boolean {
        var old: Long? = null
        var new: Long? = null
        val done =
            inner.change(on, holder, origin) { value ->
                old = value
                next(value).also { new = it }
            }
        if (done) reach(holder, origin, ChangeType.VALUE_CHANGED, old, new)
        return done
    }

    override fun remove(
        on: VariableOn,
        holder: Holder,
        origin: Int,
    ): Boolean {
        val old = inner.read(on, holder)?.value?.takeIf { entry.flags.hasValue }
        return inner.remove(on, holder, origin).also { done ->
            if (done) reach(holder, origin, ChangeType.DELETED, old, null)
        }
    }

    override fun read(
        on: VariableOn,
        holder: Holder,
    ): HeldValue? = inner.read(on, holder)

    /** A flag variable reports no value, as [PlainVariable] raises it. */
    private fun shown(
        on: VariableOn,
        holder: Holder,
    ): Long? = inner.read(on, holder)?.value?.takeIf { entry.flags.hasValue }

    private fun reach(
        holder: Holder,
        origin: Int,
        type: Int,
        old: Long?,
        new: Long?,
    ) = variables.sharedChanged(roomId, boxId, holder, ChangeOrigin.seenElsewhere(origin), type, old, new)
}
