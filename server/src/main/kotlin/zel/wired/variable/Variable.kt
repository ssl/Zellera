package zel.wired.variable

import zel.wired.firing.HeldValue

interface Variable {
    val entry: VariableEntry

    val id: String get() = entry.id

    /** 0 for a variable without a box. */
    val boxId: Int get() = 0

    fun read(
        on: VariableOn,
        holder: Holder,
    ): HeldValue?

    /** Without [override] a holder that has it keeps its value. */
    fun give(
        on: VariableOn,
        holder: Holder,
        value: Long,
        override: Boolean,
        origin: Int,
    ): Boolean = false

    fun write(
        on: VariableOn,
        holder: Holder,
        value: Long,
        origin: Int,
    ): Boolean = false

    /** One step against other rooms writing the same value. */
    fun change(
        on: VariableOn,
        holder: Holder,
        origin: Int,
        next: (Long) -> Long,
    ): Boolean {
        val old = read(on, holder) ?: return false
        return write(on, holder, next(old.value), origin)
    }

    fun remove(
        on: VariableOn,
        holder: Holder,
        origin: Int,
    ): Boolean = false

    fun holders(on: VariableOn): List<Holder> = emptyList()
}
