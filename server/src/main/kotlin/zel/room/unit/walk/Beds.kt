package zel.room.unit.walk

import zel.room.furni.RoomFurni
import zel.room.map.Point

object Beds {
    private const val EAST = 2
    private const val WEST = 6

    fun pillows(bed: RoomFurni): List<Point> {
        val anchor = bed.anchor ?: return emptyList()
        return bed.tiles().filter { if (isTurned(bed)) it.x == anchor.x else it.y == anchor.y }
    }

    fun pillowFor(
        bed: RoomFurni,
        tile: Point,
    ): Point {
        val anchor = bed.anchor ?: return tile
        return if (isTurned(bed)) Point(anchor.x, tile.y) else Point(tile.x, anchor.y)
    }

    private fun isTurned(bed: RoomFurni): Boolean = bed.direction == EAST || bed.direction == WEST
}
