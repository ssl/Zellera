package zel.room.furni

import zel.game.items.Item
import zel.protocol.generated.ObjectUpdateOutCodec
import zel.room.Room
import zel.room.RoomScope
import zel.room.furni.place.FurniWire
import zel.room.furni.place.Interactions
import zel.room.map.Point
import zel.room.tick.Audience
import zel.room.tick.Lane

/** The state is a 12-bit mask of joined sides and corners; void sides count as joined. */
object WaterPools {
    private const val STACKABLE = "stackable_water"
    private const val SHALLOW = "bw_water_1"
    private const val DEEP = "bw_water_2"

    private class Side(
        val dx: Int,
        val dy: Int,
        val corner: Boolean = false,
    )

    /** In mask bit order, highest bit first, around a 2x2 footprint. */
    private val SIDES =
        listOf(
            Side(-1, -1, corner = true),
            Side(0, -1),
            Side(1, -1),
            Side(2, -1, corner = true),
            Side(-1, 0),
            Side(2, 0),
            Side(-1, 1),
            Side(2, 1),
            Side(-1, 2, corner = true),
            Side(0, 2),
            Side(1, 2),
            Side(2, 2, corner = true),
        )

    fun isWater(definition: Item): Boolean = Interactions.of(definition) == Interactions.WATER

    fun goesOnFurni(definition: Item): Boolean = definition.name.equals(STACKABLE, ignoreCase = true)

    /** The same kind always joins; shallow pool water also takes deep pool water at its corners. */
    fun joins(
        mine: Item,
        theirs: Item,
        corner: Boolean,
    ): Boolean =
        mine.id == theirs.id ||
            (corner && isPoolWater(mine) && isPoolWater(theirs) && !mine.name.equals(DEEP, ignoreCase = true))

    fun mask(
        room: Room,
        furni: RoomFurni,
    ): Int {
        val anchor = furni.anchor ?: return 0
        return SIDES.fold(0) { mask, side ->
            val tile = Point(anchor.x + side.dx, anchor.y + side.dy)
            val joined = joinsAt(room, furni, tile, side.corner) || (!side.corner && !room.floor.isFloor(tile))
            (mask shl 1) or if (joined) 1 else 0
        }
    }

    fun refreshAround(
        scope: RoomScope,
        tiles: Collection<Point>,
    ) {
        val near = tiles.flatMap { t -> (-1..1).flatMap { dx -> (-1..1).map { Point(t.x + dx, t.y + it) } } }.toSet()
        scope.room.furni
            .all()
            .filter { isWater(it.definition) && it.anchor != null && it.tiles().any(near::contains) }
            .forEach { water ->
                if (update(scope.room, water)) {
                    scope.out.send(Lane.Now, Audience.Room, ObjectUpdateOutCodec, FurniWire.objectUpdate(water))
                }
            }
    }

    fun refreshAll(room: Room) {
        room.furni
            .all()
            .filter { isWater(it.definition) && it.anchor != null }
            .forEach { update(room, it) }
    }

    private fun update(
        room: Room,
        water: RoomFurni,
    ): Boolean {
        val state = mask(room, water).toString()
        if (water.state == state) return false
        water.state = state
        water.dirty = true
        return true
    }

    private fun joinsAt(
        room: Room,
        furni: RoomFurni,
        tile: Point,
        corner: Boolean,
    ): Boolean =
        room.furni.at(tile).furni.any {
            it !== furni && isWater(it.definition) && it.z >= furni.z && joins(furni.definition, it.definition, corner)
        }

    private fun isPoolWater(definition: Item): Boolean =
        definition.name.equals(SHALLOW, ignoreCase = true) || definition.name.equals(DEEP, ignoreCase = true)
}
