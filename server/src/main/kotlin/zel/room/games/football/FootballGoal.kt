package zel.room.games.football

import zel.room.furni.FurniBehaviour
import zel.room.furni.RoomFurni
import zel.room.games.TeamColor
import zel.room.unit.UnitDirection

class FootballGoal(
    val color: TeamColor,
) : FurniBehaviour {
    fun opensTo(
        goal: RoomFurni,
        heading: Int,
    ): Boolean {
        val facing = goal.definition.shownDirection(goal.direction)
        return Math.floorMod(heading - facing - FRONT_FIRST, UnitDirection.COUNT) < FRONT_WIDTH
    }

    private companion object {
        const val FRONT_FIRST = 3
        const val FRONT_WIDTH = 3
    }
}
