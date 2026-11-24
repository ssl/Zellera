package zel.room.chat

data class FloodRule(
    val lines: Int,
    val windowMillis: Long,
) {
    companion object {
        const val STRICT = 0
        const val NORMAL = 1
        const val LOOSE = 2

        private val BY_FILTER =
            mapOf(
                STRICT to FloodRule(3, 90_000),
                NORMAL to FloodRule(4, 100_000),
                LOOSE to FloodRule(15, 30_000),
            )

        fun of(filter: Int): FloodRule = BY_FILTER[filter] ?: BY_FILTER.getValue(NORMAL)
    }
}
