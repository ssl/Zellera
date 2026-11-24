package zel.wired.variable.box

import zel.wired.box.BoxSettings
import zel.wired.box.ConditionBox
import zel.wired.condition.Comparison
import zel.wired.firing.Firing
import zel.wired.variable.Holder
import zel.wired.variable.RoomVariables
import zel.wired.variable.VariableOn

class VariableValueMatch(
    override val furniId: Int,
    override val settings: BoxSettings,
    private val variables: RoomVariables,
) : ConditionBox {
    override fun passes(firing: Firing): Boolean {
        val target = settings.intAt(TARGET)
        if (BoxHolders.namesNoVariable(this)) return false
        val variable = variables.find(firing.scope.room, settings.variableIds.firstOrNull().orEmpty()) ?: return false
        val shape =
            OperandShape(
                fromVariable = settings.intAt(FROM_VARIABLE) != 0,
                constant = longOf(settings.intAt(HIGH), settings.intAt(LOW)),
                referenceTarget = settings.intAt(REFERENCE_TARGET),
                sameHolder = settings.intAt(SAME_HOLDER) != 0,
                target = target,
            )
        val operand = Operand.of(firing, this, variables, shape)
        // A reference read from several holders fails every comparison.
        if (operand is Operand.FromVariable && operand.sourceCount > 1) return false
        val on = VariableOn(firing.scope, firing)
        val holders = BoxHolders.of(firing, this, target)
        if (holders.isEmpty()) return BoxHolders.nonePass(this, target)
        if (!variable.entry.flags.hasValue) return false

        fun holds(
            index: Int,
            holder: Holder,
        ): Boolean {
            val left = variable.read(on, holder)?.value ?: return false
            val right = operand.valueFor(index, holder) ?: return false
            return Comparison.holds(settings.intAt(COMPARISON), left, right)
        }
        val results = holders.mapIndexed(::holds)
        return if (settings.quantifier == ANY) results.any { it } else results.all { it }
    }

    private companion object {
        const val ANY = 1
        const val TARGET = 0
        const val COMPARISON = 1
        const val FROM_VARIABLE = 2
        const val HIGH = 3
        const val LOW = 4
        const val REFERENCE_TARGET = 5
        const val SAME_HOLDER = 6
    }
}
