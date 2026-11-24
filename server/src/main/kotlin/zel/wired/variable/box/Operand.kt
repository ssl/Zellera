package zel.wired.variable.box

import zel.wired.box.WiredBox
import zel.wired.firing.Firing
import zel.wired.variable.Holder
import zel.wired.variable.RoomVariables
import zel.wired.variable.Variable
import zel.wired.variable.VariableOn

internal sealed interface Operand {
    fun valueFor(
        index: Int,
        holder: Holder,
    ): Long?

    class Constant(
        private val value: Long,
    ) : Operand {
        override fun valueFor(
            index: Int,
            holder: Holder,
        ): Long = value
    }

    class FromVariable(
        private val variable: Variable,
        private val on: VariableOn,
        private val sources: List<Holder>,
        private val sameHolder: Boolean,
    ) : Operand {
        /** How many holders the reference is read from (none for "same holder"). */
        val sourceCount: Int get() = sources.size

        override fun valueFor(
            index: Int,
            holder: Holder,
        ): Long? {
            if (sameHolder) return read(holder)
            val paired =
                when {
                    holder in sources -> holder
                    else -> sources.getOrNull(index) ?: sources.firstOrNull()
                }
            return paired?.let(::read)
        }

        private fun read(holder: Holder): Long? {
            if (!variable.entry.flags.hasValue) return null
            return variable.read(on, holder)?.value
        }
    }

    data object Nothing : Operand {
        override fun valueFor(
            index: Int,
            holder: Holder,
        ): Long? = null
    }

    companion object {
        fun of(
            firing: Firing,
            box: WiredBox,
            variables: RoomVariables,
            shape: OperandShape,
        ): Operand {
            if (!shape.fromVariable) return Constant(shape.constant)
            val room = firing.scope.room
            val id =
                box.settings.variableIds
                    .getOrNull(1)
                    .orEmpty()
            val reference = variables.find(room, id) ?: return Nothing
            if (shape.sameHolder && reference.entry.target != shape.target) return Nothing
            val sources = if (shape.sameHolder) emptyList() else BoxHolders.of(firing, box, shape.referenceTarget, 1)
            return FromVariable(reference, VariableOn(firing.scope, firing), sources, shape.sameHolder)
        }
    }
}

internal fun longOf(
    high: Int,
    low: Int,
): Long = (high.toLong() shl Int.SIZE_BITS) or (low.toLong() and INT_MASK)

private const val INT_MASK = 0xFFFFFFFFL
