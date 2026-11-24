package zel.wired.effect.user

import zel.room.games.GamesPart
import zel.wired.box.BoxSettings
import zel.wired.box.EffectBox
import zel.wired.firing.Firing

class LeaveTeam(
    override val furniId: Int,
    override val settings: BoxSettings,
    private val games: GamesPart,
) : EffectBox {
    override val negative = false
    override val needsUser = false

    override fun run(firing: Firing) =
        UserTargets.units(firing, this).forEach { unit ->
            if (unit.index !in firing.joinedTeam) games.leaveTeam(firing.scope, unit)
        }
}
