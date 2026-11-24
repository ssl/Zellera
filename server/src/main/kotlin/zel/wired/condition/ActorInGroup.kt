package zel.wired.condition

import zel.room.unit.UnitKind
import zel.wired.box.BoxSettings
import zel.wired.box.ConditionBox
import zel.wired.firing.Firing
import zel.wired.firing.Targets

class ActorInGroup(
    override val furniId: Int,
    override val settings: BoxSettings,
    private val facts: UnitFacts,
    private val negative: Boolean,
) : ConditionBox {
    override fun passes(firing: Firing): Boolean {
        val users = Targets.units(firing, this)
        if (users.isEmpty()) return Quantified.noUsers(this, negative)
        val group = groupId(firing) ?: return negative
        val positive =
            Quantified.holds(users, settings) { unit ->
                (unit.kind as? UnitKind.Player)?.let { facts.inGroup(group, it.playerId) } == true
            }
        return positive != negative
    }

    private fun groupId(firing: Firing): Int? {
        val text = settings.text.trim()
        val id = if (text.isEmpty()) firing.scope.room.info.groupId else text.toIntOrNull()
        return id?.takeIf { it > 0 }
    }
}
