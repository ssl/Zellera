package zel.wired.effect.user

import zel.room.games.ClockRun
import zel.room.games.GamesPart
import zel.wired.box.BoxSettings
import zel.wired.box.EffectBox
import zel.wired.firing.Firing

class GiveScore(
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
        val room = games.of(scope.room)
        val uses = state.of(scope.room).scoreUses.getOrPut(furniId, ::ScoreUses)
        val round = room.wiredOrNew().round
        val times = settings.ints.getOrNull(TIMES) ?: 0
        UserTargets.players(firing, this).forEach { (unit, playerId) ->
            val inTeam = room.teamOf(playerId) != null
            if (inTeam && uses.take(round, playerId, times) && run == ClockRun.Running) {
                games.giveUserScore(scope, unit, settings.ints.getOrNull(SCORE) ?: 0)
            }
        }
    }

    companion object {
        const val SCORE = 0
        const val TIMES = 1
    }
}
