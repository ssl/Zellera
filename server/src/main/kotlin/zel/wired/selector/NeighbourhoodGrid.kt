package zel.wired.selector

import zel.room.map.Point
import kotlin.math.abs

object NeighbourhoodGrid {
    const val RADIUS = 10
    private const val SIDE = 2 * RADIUS + 1
    private const val TILES = SIDE * SIDE
    private const val FIRST_INT = 3
    private const val BITS = Int.SIZE_BITS
    private val LEGS = listOf(1 to 0, 0 to -1, -1 to 0, 0 to 1)

    /** Tile offsets from the centre in spiral order; the index is the tile's rank. */
    val offsets: List<Point> = spiral()

    private val rankAt: IntArray =
        IntArray(TILES).also { ranks ->
            offsets.forEachIndexed { rank, p -> ranks[cell(p.x, p.y)] = rank }
        }

    fun chosen(
        ints: List<Int>,
        dx: Int,
        dy: Int,
    ): Boolean {
        if (abs(dx) > RADIUS || abs(dy) > RADIUS) return false
        val rank = rankAt[cell(dx, dy)]
        val word = ints.getOrNull(FIRST_INT + rank / BITS) ?: return false
        return (word ushr (rank % BITS)) and 1 == 1
    }

    private fun cell(
        dx: Int,
        dy: Int,
    ): Int = (dy + RADIUS) * SIDE + dx + RADIUS

    private fun spiral(): List<Point> {
        val tiles = ArrayList<Point>(TILES)
        var x = 0
        var y = 0
        tiles += Point(x, y)
        var leg = 0
        while (tiles.size < TILES) {
            val (stepX, stepY) = LEGS[leg % LEGS.size]
            repeat(leg / 2 + 1) {
                if (tiles.size < TILES) {
                    x += stepX
                    y += stepY
                    tiles += Point(x, y)
                }
            }
            leg++
        }
        return tiles
    }
}
