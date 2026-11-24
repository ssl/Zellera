package zel.wired.score

import zel.wired.box.BoxSettings

class LeaderboardForm(
    settings: BoxSettings,
) {
    val ints: List<Int> = settings.ints
    val ids: List<String> = settings.variableIds
    val texts: List<String> = settings.text.split(SEPARATOR)

    val sort: Int get() = int(SORT)
    val columnCount: Int get() = int(COLUMN_COUNT)
    val columnTypes: List<Int> get() = (0 until COLUMN_SLOTS).map { int(COLUMN_TYPES + it) }
    val filterOn: Boolean get() = int(FILTER_ON) == 1
    val comparisonOn: Boolean get() = int(COMPARISON_ON) == 1
    val comparisonGlobal: Boolean get() = int(COMPARISON_MODE) == 1
    val overrideCount: Int get() = int(OVERRIDE_COUNT)
    val tileX: Int get() = int(TILE_X)
    val tileY: Int get() = int(TILE_Y)
    val offsetX: Int get() = int(OFFSET_X)
    val altitude: Int get() = int(ALTITUDE)
    val widths: List<Int> get() = (0 until COLUMN_SLOTS).map { int(WIDTHS + it) }
    val pageOption: Int get() = int(PAGE)

    val filterVariable: String get() = id(0)
    val globalVariable: String get() = id(1)

    val overrides: List<Pair<String, String>>
        get() = (0 until OVERRIDE_SLOTS).map { id(FIRST_OVERRIDE_ID + it) to text(FIRST_OVERRIDE_NAME + it) }

    val title: String get() = text(0)
    val description: String get() = text(1)

    fun label(column: Int): String = text(FIRST_LABEL + column)

    val literal: String get() = text(LITERAL)

    private fun int(at: Int): Int = ints.getOrElse(at) { 0 }

    private fun id(at: Int): String = ids.getOrElse(at) { NONE }

    private fun text(at: Int): String = texts.getOrElse(at) { "" }

    companion object {
        const val SEPARATOR = "\t"
        private const val WIDEST = 3
        const val NONE = "0"
        const val COLUMN_SLOTS = 4
        const val OVERRIDE_SLOTS = 10
        const val SORT = 0
        const val COLUMN_COUNT = 1
        const val COLUMN_TYPES = 2
        const val FILTER_ON = 6
        const val COMPARISON_ON = 7
        const val COMPARISON_MODE = 8
        const val OVERRIDE_COUNT = 9
        const val TILE_X = 10
        const val TILE_Y = 11
        const val OFFSET_X = 12
        const val ALTITUDE = 13
        const val WIDTHS = 14
        const val PAGE = 18
        const val FIRST_OVERRIDE_ID = 2
        const val FIRST_LABEL = 2
        const val LITERAL = 6
        const val FIRST_OVERRIDE_NAME = 7

        const val USERNAME = 0
        const val VALUE = 1
        const val LAST_UPDATE = 2
        const val CREATION = 3
        const val LEVEL = 4
        const val TEXT = 5

        fun defaultWidth(type: Int): Int =
            when (type) {
                USERNAME -> WIDEST
                TEXT -> 2
                LAST_UPDATE, CREATION -> 1
                else -> 0
            }
    }
}
