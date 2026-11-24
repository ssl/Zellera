package zel.wired.effect.user

import zel.wired.box.BoxSettings
import zel.wired.box.EffectBox
import zel.wired.firing.Firing

class UnfreezeUser(
    override val furniId: Int,
    override val settings: BoxSettings,
    private val unfreezing: Unfreezing,
) : EffectBox {
    override val negative = false
    override val needsUser = false

    override fun run(firing: Firing) =
        UserTargets.units(firing, this).forEach { unit -> unfreezing.unfreeze(firing, unit) }
}
