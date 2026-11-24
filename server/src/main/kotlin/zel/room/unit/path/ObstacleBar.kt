package zel.room.unit.path

import zel.room.Room
import zel.room.furni.RoomFurni
import zel.room.furni.place.Interactions
import zel.room.map.Point

object ObstacleBar {
    private const val OBSTACLE = "pet.obstacle"
    private const val EAST = 2
    private const val SOUTH = 4

    fun crossed(
        room: Room,
        from: Point,
        to: Point,
    ): RoomFurni? = obstaclesAt(room, to).firstOrNull { to in middle(it) && from !in middle(it) }

    fun entered(
        room: Room,
        from: Point,
        to: Point,
    ): RoomFurni? = obstaclesAt(room, to).firstOrNull { from !in it.tiles() }

    private fun obstaclesAt(
        room: Room,
        tile: Point,
    ): List<RoomFurni> =
        room.furni
            .at(tile)
            .furni
            .filter { Interactions.of(it.definition) == OBSTACLE }

    fun middle(obstacle: RoomFurni): Set<Point> {
        val anchor = obstacle.anchor ?: return emptySet()
        val right = anchor.x + 1
        val below = anchor.y + 1
        return when (obstacle.direction) {
            EAST -> setOf(Point(right, anchor.y), Point(right, below))
            SOUTH -> setOf(Point(anchor.x, below), Point(right, below))
            else -> emptySet()
        }
    }
}
