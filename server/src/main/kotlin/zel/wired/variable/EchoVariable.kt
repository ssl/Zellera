package zel.wired.variable

import zel.wired.firing.HeldValue

class EchoVariable(
    override val entry: VariableEntry,
    override val boxId: Int,
    /** What this echoes: a variable box, or another echo. */
    val source: Variable,
) : Variable {
    override fun read(
        on: VariableOn,
        holder: Holder,
    ): HeldValue? = source.read(on, holder)

    override fun give(
        on: VariableOn,
        holder: Holder,
        value: Long,
        override: Boolean,
        origin: Int,
    ): Boolean = source.give(on, holder, value, override, origin)

    override fun write(
        on: VariableOn,
        holder: Holder,
        value: Long,
        origin: Int,
    ): Boolean = source.write(on, holder, value, origin)

    override fun change(
        on: VariableOn,
        holder: Holder,
        origin: Int,
        next: (Long) -> Long,
    ): Boolean = source.change(on, holder, origin, next)

    override fun remove(
        on: VariableOn,
        holder: Holder,
        origin: Int,
    ): Boolean = source.remove(on, holder, origin)

    override fun holders(on: VariableOn): List<Holder> = source.holders(on)
}
