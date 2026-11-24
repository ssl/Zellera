package zel.game.games.snowball.sim

import kotlin.math.abs

object Compass {
    const val SOUTH_EAST = 3

    private const val QUARTER_TURN = 90
    private const val HALF_TURN = 180
    private const val THREE_QUARTER_TURN = 270
    private const val FULL_TURN = 360
    private const val OCTANT_WIDTH = 45
    private const val FIRST_OCTANT_EDGE = 22
    private const val OCTANT_MASK = 7
    private const val SLOPE_SCALE = 256
    private const val STEEPEST_SLOPE = 255

    fun normalize(heading: Int): Int {
        val remainder = heading % FULL_TURN
        return if (heading < 0) FULL_TURN + remainder else remainder
    }

    /** Not normalized: `(0, -n)` gives 360 and `(0, 0)` gives 180; a short-axis overflow decides the quadrant. */
    fun headingOf(
        dx: Int,
        dy: Int,
    ): Int =
        if (abs(dx) <= abs(dy)) {
            mostlyVertical(dx * SLOPE_SCALE, atLeastOne(dy))
        } else {
            mostlyHorizontal(atLeastOne(dx), dy * SLOPE_SCALE)
        }

    fun octantOf(heading: Int): Int = ((normalize(heading - FIRST_OCTANT_EDGE) / OCTANT_WIDTH) + 1) and OCTANT_MASK

    // normalize keeps 360 for an exact negative turn; the tables run 0..359
    fun stepX(heading: Int): Int = ClientTables.COMPASS_X[normalize(heading) % FULL_TURN]

    fun stepY(heading: Int): Int = ClientTables.COMPASS_Y[normalize(heading) % FULL_TURN]

    private fun mostlyVertical(
        scaledX: Int,
        y: Int,
    ): Int {
        val offset = slopeDegrees(scaledX, y)
        return when {
            y < 0 -> if (scaledX > 0) offset else FULL_TURN - offset
            scaledX > 0 -> HALF_TURN - offset
            else -> HALF_TURN + offset
        }
    }

    private fun mostlyHorizontal(
        x: Int,
        scaledY: Int,
    ): Int {
        val offset = slopeDegrees(scaledY, x)
        return when {
            scaledY < 0 -> if (x > 0) QUARTER_TURN - offset else THREE_QUARTER_TURN + offset
            x > 0 -> QUARTER_TURN + offset
            else -> THREE_QUARTER_TURN - offset
        }
    }

    private fun slopeDegrees(
        scaledShortAxis: Int,
        longAxis: Int,
    ): Int = ClientTables.SLOPE_DEGREES[minOf(abs(scaledShortAxis / longAxis), STEEPEST_SLOPE)]

    private fun atLeastOne(axis: Int): Int = if (axis == 0) 1 else axis
}
