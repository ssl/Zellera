package zel.room.games.football

import zel.game.items.FurniParams
import zel.protocol.generated.ObjectUpdateOutCodec
import zel.room.RoomScope
import zel.room.event.RoomEvent
import zel.room.furni.RoomFurni
import zel.room.furni.RoomHeights
import zel.room.furni.Steppable
import zel.room.furni.Usable
import zel.room.furni.behaviour.state.StateShow
import zel.room.furni.place.FurniWire
import zel.room.games.FieldFurni
import zel.room.games.GameOut
import zel.room.games.GamesPart
import zel.room.games.StepScope
import zel.room.map.Point
import zel.room.tick.Audience
import zel.room.tick.Lane
import zel.room.tick.RoomTiming
import zel.room.unit.RoomUnit
import zel.room.unit.UnitDirection
import zel.room.unit.UnitStatus
import kotlin.math.abs

/** Rolls one ObjectUpdate per tile; half a second is shared over the tiles left; an obstacle turn costs a tile. */
abstract class RollingFurni(
    protected val part: GamesPart,
) : FieldFurni(),
    Usable,
    Steppable {
    protected abstract fun tilesOf(
        ball: RoomFurni,
        touch: BallTouch,
    ): Int

    /** The ball's params start with how far a kick and how far a push sends it. */
    protected fun named(
        ball: RoomFurni,
        index: Int,
    ): Int? = FurniParams.int(ball.definition.params, index)?.takeIf { it in 1..MAX_NAMED_TILES }

    protected abstract fun enters(
        scope: RoomScope,
        ball: RoomFurni,
        to: Point,
        heading: Int,
    ): Boolean

    /** Called before the move goes out. */
    protected open fun rollingOnto(
        scope: RoomScope,
        ball: RoomFurni,
        run: BallRun,
        to: Point,
    ) = Unit

    protected open fun touched(
        unit: RoomUnit,
        touch: BallTouch,
    ) = Unit

    protected open fun headingOff(
        unit: RoomUnit,
        next: Point,
    ): Int = unit.bodyDirection

    protected open fun meetStanding(): StandingMeet = StandingMeet.RollThrough

    override fun onStepOn(
        scope: RoomScope,
        furni: RoomFurni,
        unit: RoomUnit,
    ) {
        val lands = unit.path.lastOrNull() == furni.anchor
        val first = (part.of(scope.room).steps[unit.index] ?: 0) == 0
        val touch = if (lands && first) BallTouch.Kick else BallTouch.Push
        send(StepScope(scope), furni, unit, touch, unit.bodyDirection)
    }

    override fun onStepOff(
        scope: RoomScope,
        furni: RoomFurni,
        unit: RoomUnit,
    ) {
        val next = unit.path.firstOrNull() ?: return
        val only = unit.path.size == 1 && (part.of(scope.room).steps[unit.index] ?: 0) == 0
        val touch = if (only) BallTouch.Kick else BallTouch.StepOff
        if (!isRolling(scope, furni)) send(StepScope(scope), furni, unit, touch, headingOff(unit, next))
    }

    fun isRolling(
        scope: RoomScope,
        furni: RoomFurni,
    ): Boolean = furni.id in part.of(scope.room).ballRuns

    override fun onUse(
        scope: RoomScope,
        furni: RoomFurni,
        playerId: Int,
        param: Int,
    ) {
        val unit = scope.room.units.ofPlayer(playerId) ?: return
        val at = furni.anchor ?: return
        val states = furni.definition.stateCount
        if (states > 1 && part.context.hooks.hasRights(scope, playerId)) {
            StateShow.data(scope, furni, (((furni.state.toIntOrNull() ?: 0) + 1) % states).toString())
            scope.raise(RoomEvent.StateChanged(furni.id, unit.index))
        }
        if (abs(unit.tile.x - at.x) <= 1 && abs(unit.tile.y - at.y) <= 1) {
            send(scope, furni, unit, BallTouch.Click, unit.bodyDirection)
        }
    }

    private fun send(
        scope: RoomScope,
        ball: RoomFurni,
        unit: RoomUnit,
        touch: BallTouch,
        heading: Int,
    ) {
        val tiles = tilesOf(ball, touch)
        if (tiles == 0) return
        touched(unit, touch)
        val run =
            BallRun(
                ball.id,
                unit,
                heading,
                tiles,
                turnsAtObstacles = touch != BallTouch.Push,
                byStep = touch != BallTouch.Click,
            )
        part
            .of(scope.room)
            .ballRuns
            .put(ball.id, run)
            ?.over = true
        roll(scope, run)
    }

    private fun roll(
        scope: RoomScope,
        run: BallRun,
    ) {
        if (run.over) return
        val runs = part.of(scope.room).ballRuns
        val ball = scope.room.furni[run.ballId]
        val from = ball?.anchor
        if (ball == null || from == null) {
            runs.remove(run.ballId)
            return
        }
        var to = nextTile(scope, ball, from, run)
        if (to != null && !unitOn(scope, to, run) && standingOn(scope, to, run)) {
            to =
                when (meetStanding()) {
                    StandingMeet.Stop -> null
                    StandingMeet.Bounce -> if (run.turnsAtObstacles) bounce(scope, ball, from, run) else null
                    StandingMeet.RollThrough -> to
                }
        }
        if (to == null || unitOn(scope, to, run)) {
            runs.remove(run.ballId)
            return
        }
        run.left--
        rollingOnto(scope, ball, run, to)
        moveTo(scope, ball, from, to, run.heading)
        if (run.left <= 0) {
            runs.remove(run.ballId)
            return
        }
        val pause = PACE_MILLIS / run.left
        run.elapsed += pause
        part.context.jobs.after(scope.room, pause) { roll(it, run) }
    }

    /** A unit stepping on [tile]; the run's starter counts as stepping for one tick of the run's own pauses. */
    private fun unitOn(
        scope: RoomScope,
        tile: Point,
        run: BallRun,
    ): Boolean =
        scope.room.units.all().any {
            it.tile == tile &&
                (
                    it.isWalking || it.showsStatus(UnitStatus.MOVE) ||
                        (run.byStep && it === run.by && run.elapsed < RoomTiming.TICK_MILLIS)
                )
        }

    /** A unit standing still on [tile], other than the one whose step set the run off in its first tick. */
    private fun standingOn(
        scope: RoomScope,
        tile: Point,
        run: BallRun,
    ): Boolean =
        scope.room.units.all().any {
            it.tile == tile && !(run.byStep && it === run.by && run.elapsed < RoomTiming.TICK_MILLIS)
        }

    private fun bounce(
        scope: RoomScope,
        ball: RoomFurni,
        from: Point,
        run: BallRun,
    ): Point? {
        val back = (run.heading + HALF_TURN) % UnitDirection.COUNT
        run.heading = back
        run.left--
        return from.step(back).takeIf { run.left > 0 && enters(scope, ball, it, back) }
    }

    private fun nextTile(
        scope: RoomScope,
        ball: RoomFurni,
        from: Point,
        run: BallRun,
    ): Point? {
        val ahead = from.step(run.heading)
        if (enters(scope, ball, ahead, run.heading)) return ahead
        if (!run.turnsAtObstacles) return null
        val turn = turnsOf(run.heading).firstOrNull { enters(scope, ball, from.step(it), it) } ?: return null
        run.heading = turn
        run.left--
        return if (run.left > 0) from.step(turn) else null
    }

    private fun turnsOf(heading: Int): List<Int> =
        if (heading % 2 == 0) {
            listOf((heading + HALF_TURN) % UnitDirection.COUNT)
        } else {
            listOf(heading - QUARTER_TURN, heading + QUARTER_TURN, heading + HALF_TURN).map {
                Math.floorMod(it, UnitDirection.COUNT)
            }
        }

    private fun moveTo(
        scope: RoomScope,
        ball: RoomFurni,
        from: Point,
        to: Point,
        heading: Int,
    ) {
        val room = scope.room
        val toZ = room.furni.at(to).walkTileZ() ?: RoomHeights.stackingHeight(room, listOf(to), ball) ?: ball.z
        room.furni.move(ball, to, heading)
        ball.z = toZ
        ball.dirty = true
        scope.out.send(Lane.AfterUnitUpdate, Audience.Room, ObjectUpdateOutCodec, FurniWire.objectUpdate(ball))
        GameOut.heights(scope, listOf(from, to))
    }

    protected fun stepHeading(
        unit: RoomUnit,
        next: Point,
    ): Int = UnitDirection.between(unit.tile, next) ?: unit.bodyDirection

    protected companion object {
        const val HALF_TURN = 4
        const val KICK_PARAM = 0
        const val PUSH_PARAM = 1
        private const val MAX_NAMED_TILES = 20
        private const val QUARTER_TURN = 2

        private const val PACE_MILLIS = 500L
    }
}
