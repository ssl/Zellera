package zel.wired.variable.addon

class FxForm(
    private val ints: List<Int>,
) {
    private fun int(i: Int) = ints.getOrElse(i) { 0 }

    private fun long(
        high: Int,
        low: Int,
    ) = (int(high).toLong() shl INT_BITS) or (int(low).toLong() and LOW_HALF)

    val forUsers: Boolean get() = int(SOURCE) != 0
    val visibility: Int get() = int(VISIBILITY)
    val showMode: Int get() = int(SHOW_MODE).coerceIn(0, MAX_SHOW_MODE)
    val updateMask: Int get() = if (ints.size > UPDATE_MASK) int(UPDATE_MASK) and ALL_UPDATES else FRESH_UPDATES
    val showOnHover: Boolean get() = int(HOVER) != 0
    val showMillis: Int get() = int(SHOW_MILLIS).coerceIn(MIN_SHOW_MILLIS, MAX_SHOW_MILLIS)
    val style: Int get() = int(STYLE)
    val min: Long get() = long(MIN_HIGH, MIN_LOW)
    val max: Long get() =
        if (ints.size > MAX_LOW) {
            long(MAX_HIGH, MAX_LOW)
        } else if (ints.isEmpty()) {
            0
        } else {
            DEFAULT_MAX
        }
    val minFromVariable: Boolean get() = int(MIN_OVERRIDE) != 0
    val maxFromVariable: Boolean get() = int(MAX_OVERRIDE) != 0
    val minTarget: Int get() = int(MIN_TARGET)
    val maxTarget: Int get() = int(MAX_TARGET)
    val audienceValue: Long get() = long(AUDIENCE_HIGH, AUDIENCE_LOW)
    val subRenderer: Int get() = int(SUB)
    val segments: Int get() = int(SEGMENTS_INT)

    fun color(kind: FxKind): Int =
        when {
            ints.size <= COLOR -> 0
            int(COLOR) == UNSET -> kind.color
            int(COLOR) == kind.color -> kind.color
            else -> int(COLOR).coerceAtMost(MAX_COLOR)
        }

    fun width(kind: FxKind): Int =
        when {
            ints.size <= WIDTH -> 0
            int(WIDTH) == UNSET -> kind.width
            else -> int(WIDTH)
        }

    companion object {
        const val UNSET = -1
        const val COLOR = 7
        const val WIDTH = 8
        const val RENDERER = 9
        const val DEFAULT_MAX = 100L
        const val SUB_RENDERER = "sub_renderer"
        val RANGE_INTS = 10..13
        const val SEGMENTS = "segments"
        private const val SOURCE = 0
        private const val VISIBILITY = 1
        private const val SHOW_MODE = 2
        private const val UPDATE_MASK = 3
        private const val HOVER = 4
        private const val SHOW_MILLIS = 5
        private const val STYLE = 6
        private const val MIN_HIGH = 10
        private const val MIN_LOW = 11
        private const val MAX_HIGH = 12
        private const val MAX_LOW = 13
        private const val MIN_OVERRIDE = 14
        private const val MAX_OVERRIDE = 15
        private const val MIN_TARGET = 16
        private const val MAX_TARGET = 17
        private const val AUDIENCE_HIGH = 18
        private const val AUDIENCE_LOW = 19
        private const val SEGMENTS_INT = 20
        private const val SUB = 21
        private const val MAX_SHOW_MODE = 2
        private const val ALL_UPDATES = 15
        private const val FRESH_UPDATES = 7
        private const val MIN_SHOW_MILLIS = 1500
        private const val MAX_SHOW_MILLIS = 20000
        private const val MAX_COLOR = 1000
        private const val INT_BITS = 32
        private const val LOW_HALF = 0xFFFFFFFFL
    }
}
