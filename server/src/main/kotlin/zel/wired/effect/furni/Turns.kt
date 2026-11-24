package zel.wired.effect.furni

import zel.room.furni.RoomFurni
import zel.room.unit.UnitDirection
import kotlin.random.Random

object Turns {
    const val CLOCKWISE = 1
    const val COUNTER_CLOCKWISE = 2
    const val RANDOM = 3

    fun next(
        furni: RoomFurni,
        rotation: Int,
        random: Random,
    ): Int {
        val others = others(furni)
        return when (rotation) {
            CLOCKWISE -> others.firstOrNull()
            COUNTER_CLOCKWISE -> others.lastOrNull()
            RANDOM -> others.randomOrNull(random)
            else -> null
        } ?: furni.direction
    }

    private fun others(furni: RoomFurni): List<Int> {
        val offsets =
            if (furni.definition.hasDiagonalDirections()) {
                1 until UnitDirection.COUNT
            } else {
                2 until UnitDirection.COUNT step
                    2
            }
        return offsets
            .map { (furni.direction + it).mod(UnitDirection.COUNT) }
            .filter(furni.definition::allowsDirection)
    }
}
