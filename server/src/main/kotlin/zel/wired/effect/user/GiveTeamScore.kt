package zel.wired.effect.user

import zel.room.games.ClockRun
import zel.room.games.GamesPart
import zel.room.games.TeamColor
import zel.wired.box.BoxSettings
import zel.wired.box.EffectBox
import zel.wired.firing.Firing

class GiveTeamScore(
    override val furniId: Int,
    override val settings: BoxSettings,
    private val state: UserEffectState,
    private val games: GamesPart,
) : EffectBox {
    override val negative = false
    override val needsUser = false

    override fun run(firing: Firing) {
        val scope = firing.scope
        val run = games.clockRun(scope)
        if (run == ClockRun.Stopped) return
        val color = TeamColor.fromCode(settings.ints.getOrNull(TEAM) ?: 0) ?: return
        val times = settings.ints.getOrNull(GiveScore.TIMES) ?: 0
        val round = games.of(scope.room).wiredOrNew().round
        val uses = state.of(scope.room).scoreUses.getOrPut(furniId, ::ScoreUses)
        if (uses.take(round, ScoreUses.TEAM, times) && run == ClockRun.Running) {
            games.giveTeamScore(scope, color, settings.ints.getOrNull(GiveScore.SCORE) ?: 0)
        }
    }

    companion object {
        const val TEAM = 2
    }
}
