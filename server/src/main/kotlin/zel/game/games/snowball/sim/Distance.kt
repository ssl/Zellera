package zel.game.games.snowball.sim

import kotlin.math.abs

object Distance {
    private const val HIGHEST_BIT = 31
    private const val FIRST_SHIFTED_BUCKET = 3
    private const val UNSCALED_BUCKET = 4

    /** Approximate square root, -1 below 0: `approxSqrt(10000)` is 99. */
    fun approxSqrt(value: Int): Int {
        if (value < 0) return -1
        val bucket = maxOf(0, (HIGHEST_BIT - value.countLeadingZeroBits()) / 2 - FIRST_SHIFTED_BUCKET)
        val root = ClientTables.SQUARE_ROOTS[value shr (2 * bucket)]
        return when {
            bucket >= UNSCALED_BUCKET -> root shl (bucket - UNSCALED_BUCKET)
            else -> root shr (UNSCALED_BUCKET - bucket)
        }
    }

    fun closerThan(
        x1: Int,
        y1: Int,
        x2: Int,
        y2: Int,
        distance: Int,
    ): Boolean {
        val dx = abs(x2.toLong() - x1)
        val dy = abs(y2.toLong() - y1)
        val reach = distance.toLong()
        return dx <= reach && dy <= reach && dx * dx + dy * dy < reach * reach
    }
}
