package zel.wired.variable.box

import zel.wired.box.BoxSettings
import zel.wired.firing.Firing
import zel.wired.selector.UnitSelector
import zel.wired.variable.Holder
import zel.wired.variable.RoomVariables
import zel.wired.variable.VariableTarget

class UsersWithVariable(
    override val furniId: Int,
    override val settings: BoxSettings,
    variables: RoomVariables,
) : UnitSelector() {
    private val rule = WithVariableRule(settings, variables, VariableTarget.USER)

    override val showsFilterState get() = false

    override fun unconfigured(firing: Firing): Boolean = rule.variable(firing) == null

    override fun matches(firing: Firing): Collection<Int> {
        val held = rule.holders(firing, this)
        return firing.scope.room.units
            .all()
            .filter { Holder.of(it) in held }
            .map { it.index }
    }
}
