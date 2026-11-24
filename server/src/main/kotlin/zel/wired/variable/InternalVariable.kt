package zel.wired.variable

import zel.room.furni.RoomFurni
import zel.wired.firing.HeldValue

class InternalVariable(
    override val entry: VariableEntry,
    private val values: InternalValues,
    private val writes: InternalWrites,
) : Variable {
    override fun read(
        on: VariableOn,
        holder: Holder,
    ): HeldValue? = values.read(on, id, holder)?.let { HeldValue(it, 0, 0) }

    override fun give(
        on: VariableOn,
        holder: Holder,
        value: Long,
        override: Boolean,
        origin: Int,
    ): Boolean = writes.create(on, id, holder, value, override)

    override fun write(
        on: VariableOn,
        holder: Holder,
        value: Long,
        origin: Int,
    ): Boolean = writes.write(on, id, holder, value)

    override fun remove(
        on: VariableOn,
        holder: Holder,
        origin: Int,
    ): Boolean = writes.delete(on, id, holder)

    override fun holders(on: VariableOn): List<Holder> {
        if (id in NO_OVERVIEW) return emptyList()
        val room = on.scope.room
        return when (entry.target) {
            VariableTarget.USER -> {
                room.units
                    .all()
                    .map { Holder.of(it) }
                    .filter { read(on, it) != null }
            }

            VariableTarget.FURNI -> {
                room.furni
                    .all()
                    .map<RoomFurni, Holder> { Holder.of(it) }
                    .filter { read(on, it) != null }
            }

            else -> {
                emptyList()
            }
        }
    }

    private companion object {
        val NO_OVERVIEW = setOf("-271", "-272", "-280")
    }
}
