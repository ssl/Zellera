package zel.wired.effect.user

import zel.wired.box.BoxSettings
import zel.wired.box.EffectBox
import zel.wired.firing.Firing

class FreezeUser(
    override val furniId: Int,
    override val settings: BoxSettings,
    private val freezing: Unfreezing,
) : EffectBox {
    override val negative = false
    override val needsUser = false

    override fun run(firing: Firing) {
        val effect = EFFECTS[(settings.ints.getOrNull(EFFECT_INDEX) ?: 0).mod(EFFECTS.size)]
        val endsOnTeleport = (settings.ints.getOrNull(ENDS_ON_TELEPORT) ?: 0) != 0
        UserTargets.units(firing, this).forEach { unit ->
            freezing.freeze(firing, unit, Frozen(effect, endsOnTeleport))
        }
    }

    companion object {
        const val EFFECT_INDEX = 0
        const val ENDS_ON_TELEPORT = 1

        val EFFECTS = listOf(218, 12, 11, 53, 163)
    }
}
