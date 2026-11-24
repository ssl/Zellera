package zel.room.map

object FloorMapText {
    private const val VOID_CHAR = 'x'
    private const val LETTER_BASE = 10
    private const val RADIX = 36

    fun parseStored(text: String): TileGrid {
        val rows = rowsOf(text.replace("\r\n", "\r").replace('\n', '\r'))
        val width = rows.firstOrNull()?.length ?: 0
        val heights =
            IntArray(width * rows.size) { i ->
                val row = rows[i / width]
                if (row.length == width) heightOf(row[i % width]) ?: FloorPlan.VOID else FloorPlan.VOID
            }
        return TileGrid(width, rows.size, heights)
    }

    fun heightOf(c: Char): Int? {
        val lower = c.lowercaseChar()
        return when {
            lower == VOID_CHAR -> FloorPlan.VOID
            lower in '0'..'9' -> lower - '0'
            lower in 'a'..'z' -> lower - 'a' + LETTER_BASE
            else -> null
        }
    }

    fun render(floor: FloorPlan): String =
        buildString(floor.tileCount + floor.height) {
            for (y in 0 until floor.height) {
                for (x in 0 until floor.width) append(charOf(floor.baseHeight(Point(x, y))))
                append('\r')
            }
        }

    fun store(grid: TileGrid): String =
        (0 until grid.height).joinToString("\r") { y ->
            String(CharArray(grid.width) { x -> charOf(grid.at(Point(x, y)).takeIf { it != FloorPlan.VOID }) })
        }

    fun rowsOf(text: String): List<String> =
        text.trimEnd('\r').let { if (it.isEmpty()) emptyList() else it.split('\r') }

    private fun charOf(height: Int?): Char = height?.digitToChar(RADIX)?.lowercaseChar() ?: VOID_CHAR
}
