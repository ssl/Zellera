package zel.wired.variable.addon

import zel.wired.box.WiredBox
import zel.wired.firing.Firing
import zel.wired.variable.RoomVariables

class VariableAmounts(
    private val variables: () -> RoomVariables,
) {
    fun fromVariable(
        firing: Firing,
        box: WiredBox,
    ): Int {
        val target = box.settings.ints.getOrElse(TARGET) { 0 }
        val value = variables().readFor(firing, box, 0, target, 0, 0) ?: return 0
        return value.coerceIn(Int.MIN_VALUE.toLong(), Int.MAX_VALUE.toLong()).toInt()
    }

    private companion object {
        const val TARGET = 2
    }
}
