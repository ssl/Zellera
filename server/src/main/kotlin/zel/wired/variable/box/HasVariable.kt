package zel.wired.variable.box

import zel.wired.box.BoxSettings
import zel.wired.box.ConditionBox
import zel.wired.condition.Quantified
import zel.wired.firing.Firing
import zel.wired.variable.Holder
import zel.wired.variable.RoomVariables
import zel.wired.variable.VariableOn
import zel.wired.variable.VariableTarget

class HasVariable(
    override val furniId: Int,
    override val settings: BoxSettings,
    private val negative: Boolean,
    private val variables: RoomVariables,
) : ConditionBox {
    override fun passes(firing: Firing): Boolean {
        val target = settings.intAt(0)
        // A box that names no variable yet fails, its "not" box passes.
        if (BoxHolders.namesNoVariable(this)) return negative
        val variable =
            variables.find(firing.scope.room, settings.variableIds.firstOrNull().orEmpty()) ?: return negative
        val on = VariableOn(firing.scope, firing)
        val holders = BoxHolders.of(firing, this, target)
        // No users at all, also no triggering user, reads as an empty set.
        if (holders.isEmpty() && target == VariableTarget.USER) return Quantified.emptyHolds(settings, negative)
        if (holders.isEmpty()) return BoxHolders.nonePass(this, target, negative)

        fun has(holder: Holder) = (variable.read(on, holder) != null) != negative
        return if (settings.quantifier == ANY) holders.any(::has) else holders.all(::has)
    }

    private companion object {
        const val ANY = 1
    }
}
