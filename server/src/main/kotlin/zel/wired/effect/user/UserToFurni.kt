package zel.wired.effect.user

import zel.wired.box.BoxSettings
import zel.wired.box.EffectBox
import zel.wired.firing.Firing
import kotlin.random.Random

class UserToFurni(
    override val furniId: Int,
    override val settings: BoxSettings,
    private val moves: UserMoves,
    private val random: Random,
) : EffectBox {
    override val negative = false
    override val needsUser = false

    override fun run(firing: Firing) {
        val tiles = Destinations.of(firing, this).mapNotNull { it.anchor }
        if (tiles.isEmpty()) return
        val mode = settings.ints.firstOrNull() ?: 0
        UserTargets.units(firing, this).forEach { unit ->
            // The unit's own tile too: with physics passing it, a same-tile entry goes out (UserMoves.landing).
            moves.moveTo(firing, unit, tiles.random(random), mode)
        }
    }
}
