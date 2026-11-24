package zel.wired.selector

import zel.room.map.Point
import zel.wired.box.BoxSettings

internal class Area(
    private val x: Long,
    private val y: Long,
    private val width: Long,
    private val height: Long,
) {
    val isEmpty: Boolean get() = width <= 0 || height <= 0

    operator fun contains(tile: Point): Boolean =
        !isEmpty && tile.x >= x && tile.x < x + width && tile.y >= y && tile.y < y + height

    companion object {
        fun of(settings: BoxSettings): Area =
            Area(
                settings.intAt(X).toLong(),
                settings.intAt(Y).toLong(),
                settings.intAt(WIDTH).toLong(),
                settings.intAt(HEIGHT).toLong(),
            )

        private const val X = 0
        private const val Y = 1
        private const val WIDTH = 2
        private const val HEIGHT = 3
    }
}
