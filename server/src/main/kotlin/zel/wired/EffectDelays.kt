package zel.wired

import zel.wired.box.EffectBox
import zel.wired.firing.Firing

fun interface EffectDelays {
    fun schedule(
        firing: Firing,
        stack: WiredStack,
        delay: Int,
        effects: List<EffectBox>,
    )
}
