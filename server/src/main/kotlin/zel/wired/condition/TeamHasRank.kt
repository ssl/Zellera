package zel.wired.condition

import zel.room.games.GamesPart
import zel.room.games.TeamColor
import zel.room.unit.UnitKind
import zel.wired.box.BoxSettings
import zel.wired.box.ConditionBox
import zel.wired.firing.Firing
import zel.wired.firing.Targets

class TeamHasRank(
    override val furniId: Int,
    override val settings: BoxSettings,
    private val games: GamesPart,
) : ConditionBox {
    override fun passes(firing: Firing): Boolean {
        val room = firing.scope.room
        val game = games.of(room).roomGame()
        val named = settings.intAt(0)
        val place = settings.intAt(1) + 1
        if (named != 0) {
            val color = TeamColor.fromCode(named) ?: return false
            // With no game every team stands 1st; in a game a team without players has no rank.
            if (game == null) return place == 1
            return game.team(color)?.members?.isNotEmpty() == true && game.place(color) == place
        }
        val own = triggererTeam(firing) ?: return false
        val users = Targets.units(firing, this)
        val inTeam =
            if (users.isEmpty()) {
                Quantified.noUsersPass(this)
            } else {
                Quantified.holds(users, settings) { unit ->
                    (unit.kind as? UnitKind.Player)?.let { games.of(room).roomTeamOf(it.playerId) } == own
                }
            }
        return inTeam && (game?.place(own) ?: 1) == place
    }

    private fun triggererTeam(firing: Firing): TeamColor? {
        val unit = firing.triggeringUnit?.let { firing.scope.room.units[it] } ?: return null
        val player = unit.kind as? UnitKind.Player ?: return null
        return games.of(firing.scope.room).roomTeamOf(player.playerId)
    }
}
