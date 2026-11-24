package zel.room.games.football

import zel.room.unit.RoomUnit

/** One roll of a ball; [byStep] when [by] touched it with a step, [elapsed] sums the pauses so far. */
class BallRun(
    val ballId: Int,
    val by: RoomUnit,
    var heading: Int,
    val tiles: Int,
    val turnsAtObstacles: Boolean,
    val byStep: Boolean = false,
) {
    var left: Int = tiles
    var over: Boolean = false
    var elapsed: Long = 0
}
