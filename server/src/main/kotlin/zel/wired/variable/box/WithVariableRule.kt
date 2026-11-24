package zel.wired.variable.box

import zel.wired.box.BoxSettings
import zel.wired.box.WiredBox
import zel.wired.condition.Comparison
import zel.wired.firing.Firing
import zel.wired.selector.SelectorSources
import zel.wired.variable.Holder
import zel.wired.variable.RoomVariables
import zel.wired.variable.Variable
import zel.wired.variable.VariableOn
import zel.wired.variable.VariableTarget

internal class WithVariableRule(
    private val settings: BoxSettings,
    private val variables: RoomVariables,
    private val target: Int,
) {
    fun variable(firing: Firing): Variable? =
        variables
            .find(firing.scope.room, settings.variableIds.firstOrNull().orEmpty())
            ?.takeIf { it.entry.target == target }

    fun holders(
        firing: Firing,
        box: WiredBox,
    ): List<Holder> {
        val variable = variable(firing) ?: return emptyList()
        val on = VariableOn(firing.scope, firing)
        val all = variable.holders(on)
        val mode = settings.intAt(MODE)
        if (mode == HOLDS || !variable.entry.flags.hasValue) return all
        val right = reference(firing, on, mode, box) ?: return emptyList()
        return all.filter { holder ->
            variable.read(on, holder)?.value?.let { Comparison.holds(settings.intAt(COMPARISON), it, right) } == true
        }
    }

    private fun reference(
        firing: Firing,
        on: VariableOn,
        mode: Int,
        box: WiredBox,
    ): Long? {
        if (mode == CONSTANT) return longOf(settings.intAt(HIGH), settings.intAt(LOW))
        val reference = variables.find(firing.scope.room, settings.variableIds.getOrNull(1).orEmpty()) ?: return null
        // The reference holder follows the reference variable's kind, not the selector's: a user variable is read on
        // the box's first user (us[0]), a furni variable on its first furni (fs[0]).
        val holder =
            when (settings.intAt(REFERENCE_TARGET)) {
                VariableTarget.GLOBAL -> Holder.Room
                VariableTarget.CONTEXT -> Holder.Context
                VariableTarget.USER -> SelectorSources.units(firing, box).firstOrNull()?.let { Holder.of(it) }
                VariableTarget.FURNI -> SelectorSources.furni(firing, box).firstOrNull()?.let { Holder.of(it) }
                else -> null
            } ?: return null
        return reference.read(on, holder)?.value
    }

    private companion object {
        const val COMPARISON = 0
        const val MODE = 1
        const val HIGH = 2
        const val LOW = 3
        const val REFERENCE_TARGET = 4
        const val HOLDS = 0
        const val CONSTANT = 1
    }
}
