package zel.wired.condition

import zel.room.unit.UnitKind
import zel.wired.box.BoxSettings
import zel.wired.box.ConditionBox
import zel.wired.firing.Firing
import zel.wired.firing.Targets

class WearingBadge(
    override val furniId: Int,
    override val settings: BoxSettings,
    private val facts: UnitFacts,
    private val negative: Boolean,
) : ConditionBox {
    override fun passes(firing: Firing): Boolean {
        val code = settings.text
        val users = Targets.units(firing, this)
        // Read as "any": nobody to check fails the box and passes its not box.
        if (users.isEmpty()) return negative
        val positive =
            users.any { unit ->
                val player = unit.kind as? UnitKind.Player
                player != null && facts.wornBadges(player.playerId).any { it.equals(code, ignoreCase = true) }
            }
        return positive != negative
    }
}
