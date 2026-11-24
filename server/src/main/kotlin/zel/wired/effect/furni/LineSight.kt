package zel.wired.effect.furni

import zel.room.Room
import zel.room.map.Point
import zel.room.unit.RoomUnit
import zel.room.unit.UnitDirection
import zel.room.unit.UnitKind

/** A player in one of a furni's four straight lines, and how far along it. */
class Sighting(
    val unit: RoomUnit,
    val direction: Int,
    val distance: Int,
)

/** What a seeking furni sees: players in its own row and column, up to three tiles off. */
object LineSight {
    private const val REACH = 3
    private const val HALF_TURN = 4
    private const val QUARTER_TURN = 2
    val STRAIGHT = listOf(0, 2, 4, 6)

    /**
     * The nearest player in sight; of several as near, the one in the first direction clockwise from north. [standsOn]
     * is where a player is seen: a player mid-step is still on the tile they step from.
     */
    fun from(
        room: Room,
        anchor: Point,
        standsOn: (RoomUnit) -> Point = RoomUnit::tile,
    ): Sighting? {
        val players = room.units.all().filter { it.kind is UnitKind.Player }
        if (players.isEmpty()) return null
        return (1..REACH).firstNotNullOfOrNull { distance ->
            STRAIGHT.firstNotNullOfOrNull { direction ->
                val tile = (1..distance).fold(anchor) { at, _ -> at.step(direction) }
                players.firstOrNull { standsOn(it) == tile }?.let { Sighting(it, direction, distance) }
            }
        }
    }

    fun opposite(direction: Int): Int = (direction + HALF_TURN) % UnitDirection.COUNT

    fun sides(direction: Int): List<Int> =
        listOf(QUARTER_TURN, HALF_TURN + QUARTER_TURN).map { (direction + it) % UnitDirection.COUNT }
}
