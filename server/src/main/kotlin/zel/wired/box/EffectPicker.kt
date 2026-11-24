package zel.wired.box

import zel.wired.firing.Firing

interface EffectPicker : AddonBox {
    /** Among several pickers in a stack the highest wins. */
    val precedence: Int

    fun pick(
        effects: List<EffectBox>,
        firing: Firing,
    ): List<EffectBox>
}
