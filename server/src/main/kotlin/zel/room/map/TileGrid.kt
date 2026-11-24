package zel.room.map

class TileGrid(
    val width: Int,
    val height: Int,
    val heights: IntArray,
) {
    fun at(p: Point): Int =
        if (p.x in 0 until width && p.y in 0 until height) heights[p.y * width + p.x] else FloorPlan.VOID

    fun hasFloor(): Boolean = heights.any { it != FloorPlan.VOID }
}
