package zel.room.unit.pet

import zel.room.RoomScope
import zel.room.furni.RoomFurni
import zel.room.games.football.Football
import zel.room.games.football.RollingFurni
import zel.room.unit.RoomUnit
import zel.room.unit.UnitStatus
import kotlin.math.abs

/** One command is one kick: the pet walks onto the tile of the nearest ball, which kicks it, and plays no further. */
class PetFootball {
    fun hasBall(scope: RoomScope): Boolean = balls(scope).isNotEmpty()

    /** False for a pet that stands on the ball: it has no run-up. */
    fun start(
        scope: RoomScope,
        placed: RoomPet,
        unit: RoomUnit,
    ): Boolean {
        val ball = nearestBall(scope, unit)
        val at = ball?.anchor?.takeIf { it != unit.tile } ?: return false
        placed.ballId = ball.id
        PetLife.announce(placed, unit, at)
        return true
    }

    fun stop(placed: RoomPet) {
        placed.ballId = null
    }

    /** True while the pet is on its way to the ball; the walk's end or a ball already rolling ends the play. */
    fun tick(
        scope: RoomScope,
        placed: RoomPet,
        unit: RoomUnit,
    ): Boolean {
        val ball = placed.ballId?.let { scope.room.furni[it] }
        val rolling = (ball?.behaviour as? RollingFurni)?.isRolling(scope, ball) == true
        val onItsWay = placed.announcedGoal != null || unit.isWalking || unit.showsStatus(UnitStatus.MOVE)
        if (ball == null || rolling || !onItsWay) stop(placed)
        return placed.ballId != null
    }

    private fun balls(scope: RoomScope): List<RoomFurni> =
        scope.room.furni
            .all()
            .filter { it.anchor != null && it.behaviour is Football }

    private fun nearestBall(
        scope: RoomScope,
        unit: RoomUnit,
    ): RoomFurni? =
        balls(scope).minByOrNull { ball ->
            val at = requireNotNull(ball.anchor) { "balls stand on the floor" }
            maxOf(abs(at.x - unit.tile.x), abs(at.y - unit.tile.y))
        }
}
