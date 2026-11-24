package zel.wired.condition

import zel.room.games.GamesPart
import zel.room.unit.UnitKind
import zel.wired.box.BoxSettings
import zel.wired.box.ConditionBox
import zel.wired.firing.Firing

class TeamHasScore(
    override val furniId: Int,
    override val settings: BoxSettings,
    private val games: GamesPart,
) : ConditionBox {
    override fun passes(firing: Firing): Boolean {
        val room = firing.scope.room
        val code = settings.intAt(0)
        val color =
            if (code == OWN_TEAM) {
                val unit = firing.triggeringUnit?.let { room.units[it] }
                (unit?.kind as? UnitKind.Player)?.let { games.of(room).roomTeamOf(it.playerId) }
            } else {
                TeamColors.of(code)
            } ?: return false
        val score = games.of(room).roomGame()?.score(color) ?: 0
        return Comparison.holds(settings.intAt(COMPARISON), score, settings.intAt(1))
    }

    private companion object {
        const val COMPARISON = 2
        const val OWN_TEAM = 0
    }
}
