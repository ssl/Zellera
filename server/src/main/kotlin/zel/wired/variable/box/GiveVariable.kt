package zel.wired.variable.box

import zel.wired.box.BoxSettings
import zel.wired.box.EffectBox
import zel.wired.firing.Firing
import zel.wired.firing.Targets
import zel.wired.variable.ChangeOrigin
import zel.wired.variable.RoomVariables
import zel.wired.variable.VariableOn

class GiveVariable(
    override val furniId: Int,
    override val settings: BoxSettings,
    private val variables: RoomVariables,
) : EffectBox {
    override val negative = false
    override val needsUser = false

    override fun run(firing: Firing) {
        val target = settings.intAt(TARGET)
        val variable = variables.find(firing.scope.room, settings.variableIds.firstOrNull().orEmpty()) ?: return
        if (variable.entry.target != target || !variable.entry.flags.canCreateAndDelete) return
        val shape =
            OperandShape(
                fromVariable = settings.intAt(FROM_VARIABLE) != 0,
                constant = longOf(settings.intAt(HIGH), settings.intAt(LOW)),
                referenceTarget = settings.intAt(REFERENCE_TARGET),
                sameHolder = settings.intAt(SAME_HOLDER) != 0,
                target = target,
            )
        val operand = Targets.uncounted(firing) { Operand.of(firing, this, variables, shape) }
        val override = settings.intAt(OVERRIDE) != 0
        val on = VariableOn(firing.scope, firing)
        Targets.uncounted(firing) { BoxHolders.of(firing, this, target) }.forEachIndexed { index, holder ->
            val value =
                if (variable.entry.flags.hasValue) {
                    operand.valueFor(index, holder) ?: return@forEachIndexed
                } else {
                    0
                }
            variable.give(on, holder, value, override, ChangeOrigin.IN_ROOM)
        }
    }

    private companion object {
        const val TARGET = 0
        const val HIGH = 1
        const val LOW = 2
        const val OVERRIDE = 3
        const val FROM_VARIABLE = 4
        const val REFERENCE_TARGET = 5
        const val SAME_HOLDER = 6
    }
}
