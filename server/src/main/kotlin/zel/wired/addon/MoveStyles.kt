package zel.wired.addon

import zel.wired.effect.furni.FurniMoves
import zel.wired.firing.Firing

object MoveStyles {
    fun userDuration(firing: Firing): Int {
        val style = FurniMoves.styleOf(firing)
        return if (style.animated) style.duration else 0
    }
}
