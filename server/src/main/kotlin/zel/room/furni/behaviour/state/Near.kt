package zel.room.furni.behaviour.state

import zel.room.RoomScope
import zel.room.furni.RoomFurni
import zel.room.map.Point
import zel.room.unit.RoomUnit
import zel.room.unit.UnitDirection
import zel.room.unit.path.TileState
import zel.room.unit.path.WalkTiles
import kotlin.math.abs

object Near {
    fun adjacent(
        furni: RoomFurni,
        tile: Point,
    ): Boolean = furni.tiles().any { abs(it.x - tile.x) <= 1 && abs(it.y - tile.y) <= 1 }

    fun around(
        scope: RoomScope,
        furni: RoomFurni,
    ): List<Point> {
        val own = furni.tiles().toSet()
        return own
            .flatMap { t -> (0 until UnitDirection.COUNT).map { t.step(it) } }
            .distinct()
            .filter { it !in own && WalkTiles.at(scope.room, it).state == TileState.Open }
    }

    /** The tile of [tiles] nearest to [from] in a straight line; null when there is none. */
    fun nearest(
        tiles: Collection<Point>,
        from: Point,
    ): Point? = tiles.minByOrNull { (it.x - from.x) * (it.x - from.x) + (it.y - from.y) * (it.y - from.y) }

    fun walkBeside(
        scope: RoomScope,
        hooks: StateHooks,
        furni: RoomFurni,
        unit: RoomUnit,
    ) {
        val goal = nearest(around(scope, furni), unit.tile)
        if (goal != null && goal != unit.tile) hooks.walkTo(scope, unit, goal)
    }
}
