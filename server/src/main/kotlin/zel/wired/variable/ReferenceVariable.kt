package zel.wired.variable

import zel.wired.firing.HeldValue
import zel.wired.timing.RoomPoster

class ReferenceVariable(
    private val inner: Variable,
    private val source: SharedToken,
    private val poster: RoomPoster,
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
            if (done) notifySource(holder, ChangeType.CREATED, null, value)
        }

    override fun write(
        on: VariableOn,
        holder: Holder,
        value: Long,
        origin: Int,
    ): Boolean {
        val old = inner.read(on, holder)?.value
        return inner.write(on, holder, value, origin).also { done ->
            if (done) notifySource(holder, ChangeType.VALUE_CHANGED, old, value)
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
        if (done) notifySource(holder, ChangeType.VALUE_CHANGED, old, new)
        return done
    }

    override fun remove(
        on: VariableOn,
        holder: Holder,
        origin: Int,
    ): Boolean {
        val old = inner.read(on, holder)?.value
        return inner.remove(on, holder, origin).also { done ->
            if (done) notifySource(holder, ChangeType.DELETED, old, null)
        }
    }

    override fun read(
        on: VariableOn,
        holder: Holder,
    ): HeldValue? = inner.read(on, holder)

    private fun notifySource(
        holder: Holder,
        type: Int,
        old: Long?,
        new: Long?,
    ) {
        poster.post(source.roomId, 0) { scope ->
            val box = scope.room.furni[source.boxId] ?: return@post
            val own = variables.definition(scope.room, box) ?: return@post
            val change = VariableChange(own.id, box.id, holder, ChangeOrigin.ANOTHER_ROOM, type, old, new)
            variables.raise(VariableOn(scope), change)
        }
    }
}
