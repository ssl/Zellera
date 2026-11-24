package zel.wired.condition

import zel.room.games.GamesPart
import zel.room.unit.UnitKind
import zel.wired.box.BoxSettings
import zel.wired.box.ConditionBox
import zel.wired.firing.Firing
import zel.wired.firing.Targets

class ActorInTeam(
    override val furniId: Int,
    override val settings: BoxSettings,
    private val games: GamesPart,
    private val negative: Boolean,
) : ConditionBox {
    override fun passes(firing: Firing): Boolean {
        val code = settings.intAt(0)
        val color = TeamColors.of(code)
        val room = firing.scope.room
        val users = Targets.units(firing, this)
        if (users.isEmpty()) return Quantified.noUsers(this, negative)
        val positive =
            Quantified.holds(users, settings) { unit ->
                val player = unit.kind as? UnitKind.Player
                val team = player?.let { games.of(room).roomTeamOf(it.playerId) }
                team != null && (code == ANY_TEAM || team == color)
            }
        return positive != negative
    }

    private companion object {
        const val ANY_TEAM = 0
    }
}
