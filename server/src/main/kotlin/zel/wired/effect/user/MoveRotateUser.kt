package zel.wired.effect.user

import zel.room.map.Point
import zel.room.unit.RoomUnit
import zel.room.unit.UnitDirection
import zel.wired.box.BoxSettings
import zel.wired.box.EffectBox
import zel.wired.firing.Firing
import zel.wired.firing.Movement

class MoveRotateUser(
    override val furniId: Int,
    override val settings: BoxSettings,
    private val moves: UserMoves,
) : EffectBox {
    override val negative = false
    override val needsUser = false

    override fun run(firing: Firing) {
        val move = settings.ints.getOrNull(MOVE) ?: NONE
        val turn = settings.ints.getOrNull(TURN) ?: NONE
        UserTargets.units(firing, this).forEach { unit ->
            // A walking unit keeps its goal as user to furni mode 0 does.
            if (move in 0 until UnitDirection.COUNT) {
                moves.moveTo(firing, unit, step(unit.tile, move), UserMoves.PUSH)
            }
            if (turn in 0..TURN_COUNTER_CLOCKWISE) turn(firing, unit, turn)
        }
    }

    private fun turn(
        firing: Firing,
        unit: RoomUnit,
        turn: Int,
    ) {
        val direction =
            when (turn) {
                TURN_KEEP -> unit.bodyDirection
                TURN_CLOCKWISE -> (unit.bodyDirection + 1).mod(UnitDirection.COUNT)
                TURN_COUNTER_CLOCKWISE -> (unit.bodyDirection - 1).mod(UnitDirection.COUNT)
                else -> turn
            }
        firing.flash.movements.add(Movement.UnitTurn(unit.index, direction))
        unit.face(direction)
    }

    private fun step(
        from: Point,
        direction: Int,
    ): Point = Point(from.x + DX[direction], from.y + DY[direction])

    private companion object {
        const val MOVE = 0
        const val TURN = 1
        const val NONE = -1
        const val TURN_KEEP = 8
        const val TURN_CLOCKWISE = 9
        const val TURN_COUNTER_CLOCKWISE = 10
        val DX = intArrayOf(0, 1, 1, 1, 0, -1, -1, -1)
        val DY = intArrayOf(-1, -1, 0, 1, 1, 1, 0, -1)
    }
}
