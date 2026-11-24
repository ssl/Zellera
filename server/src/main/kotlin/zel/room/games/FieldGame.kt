package zel.room.games

import zel.game.items.FurniParams
import zel.room.RoomScope
import zel.room.furni.RoomFurni
import zel.room.unit.RoomUnit

abstract class FieldGame(
    context: GameContext,
    games: RoomGames,
    private val family: FieldFamily,
) : RoomGame(context, games) {
    protected abstract fun effectOf(
        scope: RoomScope,
        member: TeamMember,
    ): Int

    /** The effect the team's gate names after its team number, null when no gate of the team names one. */
    protected fun gateEffect(
        scope: RoomScope,
        color: TeamColor,
    ): Int? =
        scope.room.furni
            .all()
            .filter { furni ->
                (furni.behaviour as? FieldGate)?.let { it.family == family && it.color == color } == true
            }.firstNotNullOfOrNull { gate ->
                FurniParams.int(gate.definition.params, GATE_EFFECT)?.takeIf { it > 0 }
            }

    override fun playingScope(scope: RoomScope): RoomScope = (scope as? StepScope)?.direct ?: scope

    override fun join(
        scope: RoomScope,
        unit: RoomUnit,
        color: TeamColor,
    ): TeamMember? {
        val member = super.join(scope, unit, color) ?: return null
        GameOut.states(scope, teamGates(scope, color))
        joined(scope, member)
        games.teamEffect(scope, unit, effectOf(scope, member))
        return member
    }

    protected open fun joined(
        scope: RoomScope,
        member: TeamMember,
    ) = Unit

    override fun leave(
        scope: RoomScope,
        member: TeamMember,
    ) {
        super.leave(scope, member)
        GameOut.states(scope, teamGates(scope, member.color))
        if (member.present(scope)) games.teamEffect(scope, member.unit, 0)
    }

    /** A switch to another game sends no YouArePlayingGame(false) and no effect 0: the new game's effect follows. */
    override fun switchAway(
        scope: RoomScope,
        member: TeamMember,
    ) {
        team(member.color)?.leave(member)
        games.playing.remove(member.playerId)
        GameOut.states(scope, teamGates(scope, member.color))
    }

    protected fun teamGates(
        scope: RoomScope,
        color: TeamColor,
    ): List<RoomFurni> = gates(scope, games, family, color)

    protected fun changedScoreboards(
        scope: RoomScope,
        color: TeamColor? = null,
    ): List<RoomFurni> =
        scope.room.furni.all().filter { furni ->
            val board = furni.behaviour as? TeamScoreboard
            val points =
                board
                    ?.takeIf {
                        it.family == family && (color == null || it.color == color)
                    }?.let { score(it.color).toString() }
            val changed = points != null && points != furni.state
            if (changed) furni.state = points
            changed
        }

    companion object {
        private const val GATE_MAX_PLAYERS = 5
        private const val GATE_EFFECT = 1

        /**
         * The [family]'s gates of [color], set to the players they show. A TileClaim gate also counts the wired team of
         * its colour (join_team into red sets bb_gate_r to 1, a switch or leave_team sets it back). Freeze gates count
         * their own team only.
         */
        fun gates(
            scope: RoomScope,
            games: RoomGames,
            family: FieldFamily,
            color: TeamColor,
        ): List<RoomFurni> {
            val field = if (family == FieldFamily.Freeze) games.freeze else games.tileclaim
            val wired =
                if (family == FieldFamily.TileClaim) {
                    games.wired
                        ?.team(color)
                        ?.members
                        ?.size ?: 0
                } else {
                    0
                }
            val players = minOf((field?.team(color)?.members?.size ?: 0) + wired, GATE_MAX_PLAYERS).toString()
            return scope.room.furni
                .all()
                .filter {
                    (it.behaviour as? FieldGate)?.let { gate ->
                        gate.family == family && gate.color == color
                    } == true
                }.onEach { it.state = players }
        }
    }
}
