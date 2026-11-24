package zel.wired.variable.box

import zel.wired.box.WiredBox
import zel.wired.condition.Quantified
import zel.wired.firing.Firing
import zel.wired.firing.Targets
import zel.wired.variable.Holder
import zel.wired.variable.RoomVariables
import zel.wired.variable.VariableTarget

internal object BoxHolders {
    fun of(
        firing: Firing,
        box: WiredBox,
        target: Int,
        slot: Int = 0,
    ): List<Holder> =
        when (target) {
            VariableTarget.USER -> Targets.units(firing, box, slot).map { Holder.of(it) }
            VariableTarget.FURNI -> Targets.furni(firing, box, slot).map { Holder.of(it) }
            VariableTarget.GLOBAL -> listOf(Holder.Room)
            VariableTarget.CONTEXT -> listOf(Holder.Context)
            else -> emptyList()
        }

    /**
     * No holders from a user or furni set, read by the quantifier ("all" holds, "any" does not, a not box turned
     * around); an absent event holder fails. The room and the context always hold one.
     */
    fun nonePass(
        box: WiredBox,
        target: Int,
        negative: Boolean = false,
        slot: Int = 0,
    ): Boolean =
        when (target) {
            VariableTarget.USER -> Quantified.noUsers(box, negative, slot)
            VariableTarget.FURNI -> Quantified.noFurni(box, negative, slot)
            else -> false
        }

    /** A box that names no variable yet has nothing to check. */
    fun namesNoVariable(box: WiredBox): Boolean =
        box.settings.variableIds
            .firstOrNull()
            .let { it.isNullOrBlank() || it == RoomVariables.NONE }
}
