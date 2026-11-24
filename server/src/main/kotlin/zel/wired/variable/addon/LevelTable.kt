package zel.wired.variable.addon

sealed interface LevelTable {
    val maxLevel: Int
    val maxXp: Long

    fun xpForLevel(level: Int): Long

    fun read(value: Long): LevelProgress

    companion object {
        const val MANUAL = 0
        const val LINEAR = 1
        const val EXPONENTIAL = 2
        internal const val PERCENT = 100L
        private const val MIN_MAX_LEVEL = 2
        private const val FACTOR = 3
        private const val LAST = 4

        fun of(
            ints: List<Int>,
            text: String,
        ): LevelTable {
            fun int(i: Int) = ints.getOrElse(i) { 0 }
            val first = int(2).coerceAtLeast(1)
            return when (int(1)) {
                MANUAL -> ManualLevels(ManualLines.parse(text).lines)
                EXPONENTIAL -> ExponentialLevels(first, int(FACTOR).coerceAtLeast(1), maxLevel(int(LAST)))
                else -> LinearLevels(first, maxLevel(int(FACTOR)))
            }
        }

        private fun maxLevel(level: Int) = level.coerceAtLeast(MIN_MAX_LEVEL)
    }
}
