package zel.room.games.freeze

import zel.room.Room
import zel.room.map.Point
import zel.room.unit.UnitKind

object FreezeRays {
    private val STRAIGHT = listOf(0, 2, 4, 6)
    private val DIAGONAL = listOf(1, 3, 5, 7)

    fun reached(
        room: Room,
        center: Point,
        shape: BallShape,
    ): Map<Point, Int> {
        val tiles = linkedMapOf(center to 0)
        val directions = if (shape.straight) STRAIGHT else emptyList()
        (directions + if (shape.diagonal) DIAGONAL else emptyList()).forEach {
            addRay(
                room,
                tiles,
                center,
                it,
                shape.reach,
            )
        }
        return tiles
    }

    private fun addRay(
        room: Room,
        tiles: MutableMap<Point, Int>,
        center: Point,
        direction: Int,
        reach: Int,
    ) {
        var tile = center
        for (distance in 1..reach) {
            tile = tile.step(direction)
            if (!room.floor.isFloor(tile)) return
            val stack = room.furni.at(tile).furni
            if (stack.any { it.behaviour is FreezeBlock && (it.state.isEmpty() || it.state == "0") }) {
                if (stack.any { it.behaviour is FreezeTile }) tiles.putIfAbsent(tile, distance)
                return
            }
            tiles.putIfAbsent(tile, distance)
            if (room.units.all().any { it.tile == tile && it.kind is UnitKind.Player }) return
        }
    }
}
