package zel.wired.variable

import zel.wired.firing.HeldValue

class PointerVariable(
    override val entry: VariableEntry,
    override val boxId: Int,
) : Variable {
    override fun read(
        on: VariableOn,
        holder: Holder,
    ): HeldValue? = null
}
