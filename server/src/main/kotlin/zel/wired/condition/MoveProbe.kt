package zel.wired.condition

import zel.wired.firing.Firing

fun interface MoveProbe {
    fun completes(
        firing: Firing,
        conditionId: Int,
    ): Boolean

    companion object {
        val NONE: MoveProbe = MoveProbe { _, _ -> true }
    }
}
