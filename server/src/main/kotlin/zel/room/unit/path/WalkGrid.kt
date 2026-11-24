package zel.room.unit.path

import zel.room.Room
import zel.room.map.FloorPlan
import zel.room.map.Point
import kotlin.math.abs

class WalkGrid(
    val floor: FloorPlan,
    private val unitsAt: Map<Point, List<Int>>,
    val walkThrough: Boolean,
    private val tileOf: (Point) -> WalkTile,
) {
    private val tiles = HashMap<Point, WalkTile>()

    fun tile(p: Point): WalkTile = if (floor.contains(p)) tiles.getOrPut(p) { tileOf(p) } else WalkTile.VOID

    fun holdsUnit(
        p: Point,
        mover: Mover,
    ): Boolean = unitsAt[p].orEmpty().any { it !in mover.ignoredUnits }

    /** The tiles units stand on. */
    fun unitTiles(): Set<Point> = unitsAt.keys

    /** Within two straight steps of the door units never block, so the entrance cannot be plugged. */
    fun nearDoor(p: Point): Boolean = nearDoor(p.x, p.y)

    fun nearDoor(
        x: Int,
        y: Int,
    ): Boolean {
        val door = floor.door.tile
        return abs(x - door.x) + abs(y - door.y) <= DOOR_RANGE
    }

    companion object {
        private const val DOOR_RANGE = 2

        fun of(room: Room): WalkGrid =
            WalkGrid(
                floor = room.floor,
                unitsAt = room.units.all().groupBy({ it.tile }, { it.index }),
                walkThrough = room.info.allowWalkThrough,
            ) { WalkTiles.at(room, it) }
    }
}
