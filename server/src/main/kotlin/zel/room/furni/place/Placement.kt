package zel.room.furni.place

import zel.room.map.Point
import kotlin.math.abs

sealed interface Placement {
    val itemId: Int

    data class Floor(
        override val itemId: Int,
        val tile: Point,
        val direction: Int,
    ) : Placement

    data class Wall(
        override val itemId: Int,
        val position: String,
    ) : Placement

    companion object {
        private const val FLOOR_PARTS = 4

        fun parse(text: String): Placement? {
            val stripId = text.substringBefore(' ').toIntOrNull() ?: return null
            val rest = text.substringAfter(' ', "")
            if (rest.startsWith(":")) {
                return if (WallPosition.isValid(rest)) Wall(abs(stripId), rest) else null
            }
            val parts = text.split(' ')
            if (parts.size != FLOOR_PARTS) return null
            val x = parts[1].toIntOrNull() ?: return null
            val y = parts[2].toIntOrNull() ?: return null
            val direction = parts[3].toIntOrNull() ?: return null
            return Floor(abs(stripId), Point(x, y), direction)
        }
    }
}
