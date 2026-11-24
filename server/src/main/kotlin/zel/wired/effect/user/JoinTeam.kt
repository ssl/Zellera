package zel.wired.effect.user

import zel.room.RoomScope
import zel.room.games.GamesPart
import zel.room.games.TeamColor
import zel.room.unit.RoomUnit
import zel.room.unit.UnitKind
import zel.wired.box.BoxSettings
import zel.wired.box.EffectBox
import zel.wired.firing.Firing

class JoinTeam(
    override val furniId: Int,
    override val settings: BoxSettings,
    private val games: GamesPart,
) : EffectBox {
    override val negative = false
    override val needsUser = false

    override fun run(firing: Firing) {
        val color = TeamColor.fromCode(settings.ints.getOrNull(TEAM) ?: 0) ?: return
        val game = settings.ints.getOrNull(GAME) ?: 0
        UserTargets.units(firing, this).forEach { unit ->
            val playerId = (unit.kind as? UnitKind.Player)?.playerId
            if (playerId != null) {
                join(firing.scope, unit, playerId, color, game)
            } else {
                games.botJoinsTeam(firing.scope, unit, color)
            }
            firing.joinedTeam += unit.index
        }
    }

    @Suppress("LongParameterList") // who joins which team of which game
    private fun join(
        scope: RoomScope,
        unit: RoomUnit,
        playerId: Int,
        color: TeamColor,
        game: Int,
    ) {
        val room = games.of(scope.room)
        val field =
            when (game) {
                TILECLAIM -> room.tileclaimOrNew()
                FREEZE -> room.freezeOrNew()
                else -> return games.joinTeam(scope, unit, color)
            }
        if (field.memberOf(playerId)?.color != color) field.join(scope, unit, color)
    }

    companion object {
        const val TEAM = 0
        const val GAME = 1
        const val TILECLAIM = 1
        const val FREEZE = 2
    }
}
