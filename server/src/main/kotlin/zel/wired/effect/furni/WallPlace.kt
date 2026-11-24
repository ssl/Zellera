package zel.wired.effect.furni

import zel.room.Room
import zel.room.furni.RoomFurni
import zel.room.map.Point
import kotlin.math.roundToInt

data class WallPlace(
    val tile: Point,
    val localX: Int,
    val localY: Int,
    val right: Boolean,
    val altitude: Int?,
) {
    fun text(): String {
        val side = if (right) 'r' else 'l'
        val tail = altitude?.let { " a=$it" }.orEmpty()
        return ":w=${tile.x},${tile.y} l=$localX,$localY $side$tail"
    }

    fun altitude(room: Room): Int =
        altitude ?: ((base(room, tile) - (localY - sideOffset(room)) / pixelsPerUnit(room)) * HUNDRED).roundToInt()

    fun localYAt(
        room: Room,
        altitude: Int,
    ): Int = (sideOffset(room) + (base(room, tile) - altitude / HUNDRED) * pixelsPerUnit(room)).roundToInt()

    private fun sideOffset(room: Room): Double {
        val width = if (room.floor.smallScale) SMALL_WIDTH else WIDTH
        return if (right) localX / 2.0 else (width - localX) / 2.0
    }

    private fun pixelsPerUnit(room: Room): Double =
        if (room.floor.smallScale) SMALL_PIXELS_PER_UNIT else PIXELS_PER_UNIT

    companion object {
        const val WIDTH = 32
        const val SMALL_WIDTH = 16
        private const val PIXELS_PER_UNIT = 32.0
        private const val SMALL_PIXELS_PER_UNIT = 16.0
        private const val HUNDRED = 100.0
        private const val VOID_BASE = 3.6
        private const val AUTOMATIC_RISE_CAP = 20
        private const val WALL_X = 1
        private const val WALL_Y = 2
        private const val LOCAL_X = 3
        private const val LOCAL_Y = 4
        private const val SIDE = 5
        private const val ALTITUDE = 6
        private val PATTERN = Regex(""":w=(-?\d+),(-?\d+) l=(-?\d+),(-?\d+) ([lr])(?: a=(-?\d+))?""")

        fun of(item: RoomFurni): WallPlace? = item.wallPosition?.let(::parse)

        fun parse(text: String): WallPlace? {
            val g = PATTERN.matchEntire(text)?.groupValues ?: return null
            return WallPlace(
                Point(g[WALL_X].toInt(), g[WALL_Y].toInt()),
                g[LOCAL_X].toInt(),
                g[LOCAL_Y].toInt(),
                g[SIDE] == "r",
                g[ALTITUDE].toIntOrNull(),
            )
        }

        private fun base(
            room: Room,
            tile: Point,
        ): Double {
            val floor = room.floor
            return when {
                !floor.contains(tile) -> 0.0
                else -> floor.floorZ(tile) ?: (VOID_BASE + voidRise(room))
            }
        }

        /** Two per fixed wall level; an automatic wall rises with the highest floor tile, at most 20. */
        private fun voidRise(room: Room): Double =
            if (room.floor.fixedWallHeight >= 0) {
                2.0 * room.floor.fixedWallHeight
            } else {
                (room.floor.highestFloor() ?: 0).coerceAtMost(AUTOMATIC_RISE_CAP).toDouble()
            }
    }
}
