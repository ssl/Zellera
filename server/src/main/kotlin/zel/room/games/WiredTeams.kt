package zel.room.games

import zel.game.effects.EffectRoles
import zel.room.RoomScope
import zel.room.unit.RoomUnit
import zel.room.unit.UnitKind

class WiredTeams(
    context: GameContext,
    games: RoomGames,
) : RoomGame(context, games) {
    override val scoreName = "WiredGame"

    override fun member(
        unit: RoomUnit,
        playerId: Int,
        color: TeamColor,
    ): TeamMember = TeamMember(unit, playerId, color)

    fun newRound(scope: RoomScope) = startRound(scope)

    /**
     * A switch from another wired team sends only YouArePlayingGame(true) and the new team's effect, no
     * YouArePlayingGame(false) or effect 0 for the old team first.
     */
    override fun join(
        scope: RoomScope,
        unit: RoomUnit,
        color: TeamColor,
    ): TeamMember? {
        val switched = (unit.kind as? UnitKind.Player)?.playerId?.let(::memberOf)
        switched?.let { old ->
            team(old.color)?.leave(old)
            GameOut.states(scope, FieldGame.gates(scope, games, FieldFamily.TileClaim, old.color))
        }
        val member = super.join(scope, unit, color) ?: return null
        GameOut.states(scope, FieldGame.gates(scope, games, FieldFamily.TileClaim, color))
        games.teamEffect(scope, unit, effectOf(color))
        return member
    }

    override fun leave(
        scope: RoomScope,
        member: TeamMember,
    ) {
        super.leave(scope, member)
        GameOut.states(scope, FieldGame.gates(scope, games, FieldFamily.TileClaim, member.color))
        if (member.present(scope)) games.teamEffect(scope, member.unit, 0)
    }

    override fun switchAway(
        scope: RoomScope,
        member: TeamMember,
    ) {
        team(member.color)?.leave(member)
        games.playing.remove(member.playerId)
        GameOut.states(scope, FieldGame.gates(scope, games, FieldFamily.TileClaim, member.color))
    }

    override fun endRound(scope: RoomScope) = Unit

    companion object {
        fun effectOf(color: TeamColor): Int = EffectRoles.of(EffectRoles.TEAM + color.name.lowercase())
    }
}
