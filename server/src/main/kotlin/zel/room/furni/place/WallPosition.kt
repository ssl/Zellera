package zel.room.furni.place

import zel.room.map.Point

object WallPosition {
    private const val MAX_LENGTH = 32
    private val PATTERN = Regex(":w=-?\\d{1,3},-?\\d{1,3} l=-?\\d{1,4},-?\\d{1,4} [lr]")
    private val TILE = Regex("^:w=(-?\\d+),(-?\\d+) ")

    fun isValid(position: String): Boolean = position.length <= MAX_LENGTH && PATTERN.matches(position)

    /** The wall tile a wall item's position names (`:w=x,y ...`), or null when it names none. */
    fun tile(position: String?): Point? {
        val g = position?.let(TILE::find)?.groupValues ?: return null
        return Point(g[1].toInt(), g[2].toInt())
    }
}
