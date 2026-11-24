package zel.room.games.football

import zel.game.quests.CountedAction
import zel.room.RoomScope
import zel.room.furni.RoomFurni
import zel.room.furni.behaviour.state.StateShow
import zel.room.games.GamesPart
import zel.room.games.tileclaim.TileClaimTile
import zel.room.map.Point
import zel.room.unit.RoomUnit
import zel.room.unit.UnitKind
import zel.room.unit.path.TileState
import zel.room.unit.path.WalkTiles

/** Rolls like the football, colouring patches; its state is the tiles left and the kicker's team (0 for none). */
class Puck(
    part: GamesPart,
) : RollingFurni(part) {
    override fun tilesOf(
        ball: RoomFurni,
        touch: BallTouch,
    ): Int =
        when (touch) {
            BallTouch.Kick -> named(ball, KICK_PARAM) ?: KICK_TILES
            BallTouch.Click -> CLICK_TILES
            BallTouch.Push -> named(ball, PUSH_PARAM) ?: 1
            BallTouch.StepOff -> 0
        }

    override fun touched(
        unit: RoomUnit,
        touch: BallTouch,
    ) {
        val playerId = (unit.kind as? UnitKind.Player)?.playerId ?: return
        part.context.host.counted(playerId, CountedAction.TILECLAIM_PUSH_PUCK, 1)
    }

    override fun enters(
        scope: RoomScope,
        ball: RoomFurni,
        to: Point,
        heading: Int,
    ): Boolean {
        if (WalkTiles.at(scope.room, to).state != TileState.Open) return false
        val top =
            scope.room.furni
                .at(to)
                .topItem(ball) ?: return true
        return top.isStackable && !top.definition.canSit && !top.definition.canLay
    }

    override fun rollingOnto(
        scope: RoomScope,
        ball: RoomFurni,
        run: BallRun,
        to: Point,
    ) {
        val playerId = (run.by.kind as? UnitKind.Player)?.playerId
        val member = playerId?.let { part.of(scope.room).tileclaim?.memberOf(it) }
        if (playerId != null && member != null) {
            val top =
                scope.room.furni
                    .at(to)
                    .topItem(ball)
            (top?.behaviour as? TileClaimTile)?.step(scope, top, playerId)
        }
        StateShow.set(ball, "${run.left.coerceAtLeast(1)}${member?.color?.code ?: NO_TEAM}")
    }

    private companion object {
        const val KICK_TILES = 6
        const val CLICK_TILES = 3
        const val NO_TEAM = 0
    }
}
