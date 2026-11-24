package zel.room.games.football

import zel.game.achievements.GameEvents
import zel.room.RoomScope
import zel.room.furni.RoomFurni
import zel.room.furni.behaviour.state.StateShow
import zel.room.games.GameOut
import zel.room.games.GamesPart
import zel.room.games.TeamColor
import zel.room.map.Point
import zel.room.unit.RoomUnit
import zel.room.unit.UnitDirection
import zel.room.unit.UnitKind
import zel.room.unit.path.PathSearch

/** Kick 6 tiles, click 3, push 1; its state shows the tiles left doubled ("55" to "11") and keeps the last. */
class Football(
    part: GamesPart,
) : RollingFurni(part) {
    override fun tilesOf(
        ball: RoomFurni,
        touch: BallTouch,
    ): Int =
        when (touch) {
            BallTouch.Kick -> named(ball, KICK_PARAM) ?: KICK_TILES
            BallTouch.Click -> CLICK_TILES
            BallTouch.Push, BallTouch.StepOff -> named(ball, PUSH_PARAM) ?: 1
        }

    override fun headingOff(
        unit: RoomUnit,
        next: Point,
    ): Int = (stepHeading(unit, next) + HALF_TURN) % UnitDirection.COUNT

    override fun meetStanding(): StandingMeet = StandingMeet.entries.random(part.context.random)

    override fun enters(
        scope: RoomScope,
        ball: RoomFurni,
        to: Point,
        heading: Int,
    ): Boolean {
        if (!scope.room.floor.isFloor(to)) return false
        val stack = scope.room.furni.at(to)
        val top = stack.topItem(ball) ?: return true
        val goal = top.behaviour as? FootballGoal
        return stack.furni.all { it === ball || it.isStackable } &&
            top.top - ball.z < PathSearch.MAX_STEP_UP - HEIGHT_SLACK &&
            (goal == null || goal.opensTo(top, heading))
    }

    override fun rollingOnto(
        scope: RoomScope,
        ball: RoomFurni,
        run: BallRun,
        to: Point,
    ) {
        val goal =
            scope.room.furni
                .at(to)
                .topItem(ball)
                ?.behaviour as? FootballGoal
        if (goal != null && lyingOn(scope, ball)?.behaviour !is FootballGoal) score(scope, run.by, goal.color)
        val left = run.left.coerceAtLeast(1)
        StateShow.set(ball, "$left$left")
    }

    private fun lyingOn(
        scope: RoomScope,
        ball: RoomFurni,
    ): RoomFurni? =
        ball.anchor?.let {
            scope.room.furni
                .at(it)
                .topItem(ball)
        }

    private fun score(
        scope: RoomScope,
        kicker: RoomUnit,
        color: TeamColor,
    ) {
        val playerId = (kicker.kind as? UnitKind.Player)?.playerId
        val owner = scope.room.info.ownerId
        val hooks = part.context.hooks
        if (playerId != null) {
            part.context.jobs.after(scope.room, GOAL_ACHIEVEMENT_DELAY_MS) {
                hooks.progress(playerId, GameEvents.GAME_FOOTBALL_GOAL, 1)
                hooks.progress(owner, GameEvents.GAME_FOOTBALL_GOAL_IN_ROOM, 1)
            }
        }
        GameOut.wave(scope, kicker.index)
        scope.room.furni
            .all()
            .filter { (it.behaviour as? FootballScoreboard)?.color == color }
            .forEach { FootballScoreboard.change(scope, it, 1) }
    }

    private companion object {
        const val KICK_TILES = 6
        const val CLICK_TILES = 3
        const val HEIGHT_SLACK = 1e-6

        const val GOAL_ACHIEVEMENT_DELAY_MS = 10_000L
    }
}
