package zel.room.map

class FloorPlan(
    val width: Int,
    val height: Int,
    private val baseHeights: IntArray,
    val door: Door,
    val smallScale: Boolean,
    val fixedWallHeight: Int,
    /** A height step is 1/[parts] of a unit. */
    private val parts: Int = 1,
) {
    init {
        require(baseHeights.size == width * height) { "floor plan has ${baseHeights.size} tiles, not $width x $height" }
    }

    val tileCount: Int get() = baseHeights.size

    fun contains(p: Point): Boolean = p.x in 0 until width && p.y in 0 until height

    fun baseHeight(p: Point): Int? = stored(p)?.let { Math.floorDiv(it, parts) }

    fun floorZ(p: Point): Double? = stored(p)?.let { if (parts == 1) it.toDouble() else it.toDouble() / parts }

    /** False for a floor larger than the client's floor map can carry. */
    val fitsClient: Boolean get() = tileCount + height <= MAP_TEXT_LIMIT

    private fun stored(p: Point): Int? = if (contains(p)) baseHeights[p.y * width + p.x].takeIf { it != VOID } else null

    fun isFloor(p: Point): Boolean = stored(p) != null

    /** Every tile of the rectangle, row by row, x fastest (the HeightMap order). */
    fun tiles(): List<Point> = List(tileCount) { Point(it % width, it / width) }

    fun highestFloor(): Int? = baseHeights.maxOrNull()?.takeIf { it != VOID }?.let { Math.floorDiv(it, parts) }

    companion object {
        const val VOID = -1
        const val MAP_TEXT_LIMIT = 0xFFFF
    }
}
