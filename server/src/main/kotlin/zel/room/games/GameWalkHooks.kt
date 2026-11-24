package zel.room.games

import zel.room.RoomScope
import zel.room.map.Point
import zel.room.unit.RoomUnit
import zel.room.unit.walk.WalkHooks

class GameWalkHooks(
    private val part: () -> GamesPart?,
    private val next: WalkHooks = WalkHooks.None,
) : WalkHooks by next {
    override fun mayWalk(unit: RoomUnit): Boolean = part()?.holds(unit) != true && next.mayWalk(unit)

    override fun stepped(
        scope: RoomScope,
        unit: RoomUnit,
        onto: Point,
    ) {
        part()?.stepped(scope, unit, onto)
        next.stepped(scope, unit, onto)
    }

    override fun walkEnded(
        scope: RoomScope,
        unit: RoomUnit,
        steps: Int,
    ) {
        part()?.walkEnded(scope, unit)
        next.walkEnded(scope, unit, steps)
    }
}
