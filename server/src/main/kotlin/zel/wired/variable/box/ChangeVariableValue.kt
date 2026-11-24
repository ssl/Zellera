package zel.wired.variable.box

import zel.wired.box.BoxSettings
import zel.wired.box.EffectBox
import zel.wired.firing.Firing
import zel.wired.firing.Targets
import zel.wired.variable.ChangeOrigin
import zel.wired.variable.Holder
import zel.wired.variable.InternalIds
import zel.wired.variable.RoomVariables
import zel.wired.variable.VariableOn
import kotlin.random.Random

class ChangeVariableValue(
    override val furniId: Int,
    override val settings: BoxSettings,
    private val variables: RoomVariables,
    private val random: Random = Random.Default,
) : EffectBox {
    override val negative = false
    override val needsUser = false

    override fun run(firing: Firing) {
        val target = settings.intAt(TARGET)
        val variable = variables.find(firing.scope.room, settings.variableIds.firstOrNull().orEmpty()) ?: return
        if (variable.entry.target != target || !variable.entry.flags.canWriteValue) return
        val shape =
            OperandShape(
                fromVariable = settings.intAt(FROM_VARIABLE) != 0,
                constant = longOf(settings.intAt(HIGH), settings.intAt(LOW)),
                referenceTarget = settings.intAt(REFERENCE_TARGET),
                sameHolder = settings.intAt(SAME_HOLDER) != 0,
                target = target,
            )
        val operand = Targets.uncounted(firing) { Operand.of(firing, this, variables, shape) }
        val operation = settings.intAt(OPERATION)
        val on = VariableOn(firing.scope, firing)
        Targets.uncounted(firing) { BoxHolders.of(firing, this, target) }.forEachIndexed { index, holder ->
            val missing = variable.read(on, holder) == null
            if (missing && !(variable.id == InternalIds.HAND_ITEM && holder !is Holder.Furni)) return@forEachIndexed
            // An operand with no value writes nothing: no blink, no variable changed.
            val by =
                (if (ValueOps.takesOperand(operation)) operand.valueFor(index, holder) else 0L)
                    ?: return@forEachIndexed
            // A user without `@handitem` reads it as 0: the change hands the result and holds `@handitem` from then
            // on.
            if (missing) {
                variable.give(on, holder, ValueOps.apply(operation, 0L, by, random), true, ChangeOrigin.IN_ROOM)
                return@forEachIndexed
            }
            variable.change(on, holder, ChangeOrigin.IN_ROOM) { old -> ValueOps.apply(operation, old, by, random) }
        }
    }

    private companion object {
        const val TARGET = 0
        const val OPERATION = 1
        const val FROM_VARIABLE = 2
        const val HIGH = 3
        const val LOW = 4
        const val REFERENCE_TARGET = 5
        const val SAME_HOLDER = 6
    }
}
