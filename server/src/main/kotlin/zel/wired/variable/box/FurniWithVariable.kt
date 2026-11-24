package zel.wired.variable.box

import zel.wired.box.BoxSettings
import zel.wired.firing.Firing
import zel.wired.selector.FurniSelector
import zel.wired.variable.Holder
import zel.wired.variable.RoomVariables
import zel.wired.variable.VariableTarget

class FurniWithVariable(
    override val furniId: Int,
    override val settings: BoxSettings,
    variables: RoomVariables,
) : FurniSelector() {
    private val rule = WithVariableRule(settings, variables, VariableTarget.FURNI)

    override val showsFilterState get() = false

    override val wallItems get() = true

    override fun unconfigured(firing: Firing): Boolean = rule.variable(firing) == null

    override fun matches(firing: Firing): Collection<Int> =
        rule
            .holders(firing, this)
            .mapNotNull { (it as? Holder.Furni)?.id }
}
