package zel.wired.addon

import zel.wired.box.BoxSettings
import zel.wired.box.EffectBox
import zel.wired.box.EffectPicker
import zel.wired.firing.Firing

class ExecuteInOrder(
    override val furniId: Int,
    override val settings: BoxSettings,
) : EffectPicker {
    override val precedence = PRECEDENCE

    override fun pick(
        effects: List<EffectBox>,
        firing: Firing,
    ): List<EffectBox> = effects.groupBy { it.settings.delay }.values.flatten()

    companion object {
        const val PRECEDENCE = 1
    }
}
