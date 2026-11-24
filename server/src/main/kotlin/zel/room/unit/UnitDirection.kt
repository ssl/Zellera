package zel.room.unit

import zel.room.map.Point
import kotlin.math.sign

object UnitDirection {
    /** Number of compass directions a unit or furni can face. */
    const val COUNT = 8

    private const val ROW = 3

    private val BY_SIGNS = intArrayOf(7, 0, 1, 6, -1, 2, 5, 4, 3)

    fun between(
        from: Point,
        to: Point,
    ): Int? = BY_SIGNS[((to.y - from.y).sign + 1) * ROW + (to.x - from.x).sign + 1].takeIf { it >= 0 }

    fun isDiagonal(direction: Int): Boolean = Math.floorMod(direction, COUNT) % 2 == 1
}
