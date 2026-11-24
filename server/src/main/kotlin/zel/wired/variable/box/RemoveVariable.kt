package zel.wired.variable.box

import zel.wired.box.BoxSettings
import zel.wired.box.EffectBox
import zel.wired.firing.Firing
import zel.wired.firing.Targets
import zel.wired.variable.ChangeOrigin
import zel.wired.variable.RoomVariables
import zel.wired.variable.VariableOn

class RemoveVariable(
    override val furniId: Int,
    override val settings: BoxSettings,
    private val variables: RoomVariables,
) : EffectBox {
    override val negative = false
    override val needsUser = false

    override fun run(firing: Firing) {
        val target = settings.intAt(0)
        val variable = variables.find(firing.scope.room, settings.variableIds.firstOrNull().orEmpty()) ?: return
        if (variable.entry.target != target || !variable.entry.flags.canCreateAndDelete) return
        val on = VariableOn(firing.scope, firing)
        val holders = Targets.uncounted(firing) { BoxHolders.of(firing, this, target) }
        holders.forEach { variable.remove(on, it, ChangeOrigin.IN_ROOM) }
    }
}
