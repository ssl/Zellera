package zel.wired.timing

import zel.wired.box.BoxSettings
import zel.wired.box.EffectBox
import zel.wired.firing.Firing

class ResetTimers(
    override val furniId: Int,
    override val settings: BoxSettings,
    private val timers: WiredTimers,
) : EffectBox {
    override val negative = false
    override val needsUser = false

    override fun run(firing: Firing) = timers.reset(firing.scope.room)
}
