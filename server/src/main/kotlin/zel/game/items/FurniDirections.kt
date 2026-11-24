package zel.game.items

import kotlin.math.abs

@JvmInline
value class FurniDirections(
    private val mask: Int,
) {
    private fun has(direction: Int) = mask and (1 shl direction) != 0

    val hasDiagonals: Boolean get() = mask and DIAGONALS != 0

    /** The directions the furni's art is drawn in, lowest first; empty when not known. */
    val drawn: List<Int> get() = (0 until COUNT).filter(::has)

    fun allows(direction: Int): Boolean = direction in 0 until COUNT && (mask == 0 || has(direction))

    fun shown(direction: Int): Int {
        val asked = Math.floorMod(direction, COUNT)
        if (mask == 0 || has(asked)) return asked
        return (0 until COUNT).filter(::has).minByOrNull { candidate ->
            val apart = abs(candidate - asked)
            minOf(apart, COUNT - apart)
        } ?: asked
    }

    private companion object {
        const val COUNT = 8
        const val DIAGONALS = 0xAA
    }
}
