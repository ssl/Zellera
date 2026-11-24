package zel.game.items

/** Heights the way the game adds and writes them: as single-precision floats. */
object FurniHeights {
    /** Adding zero keeps the other number exactly. */
    fun stack(
        base: Double,
        height: Double,
    ): Double =
        when {
            height == 0.0 -> base
            base == 0.0 -> height
            else -> (base.toFloat() + height.toFloat()).toDouble()
        }

    fun formatZ(z: Double): String = z.toFloat().toString()

    fun formatHeight(height: Double): String = height.toFloat().toString()
}
