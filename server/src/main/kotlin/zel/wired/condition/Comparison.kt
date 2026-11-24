package zel.wired.condition

object Comparison {
    const val LESS = 0
    const val EQUAL = 1
    const val MORE = 2
    private const val LESS_OR_EQUAL = 3
    private const val NOT_EQUAL = 4
    private const val MORE_OR_EQUAL = 5

    /** An unknown code never holds. */
    fun holds(
        code: Int,
        left: Long,
        right: Long,
    ): Boolean =
        when (code) {
            LESS -> left < right
            EQUAL -> left == right
            MORE -> left > right
            LESS_OR_EQUAL -> left <= right
            NOT_EQUAL -> left != right
            MORE_OR_EQUAL -> left >= right
            else -> false
        }

    fun anyOfSix(code: Int): Int = code.takeIf { it in LESS..MORE_OR_EQUAL } ?: EQUAL

    /** A box with only the less / equal / more radio reads any other value as equal. */
    fun radio(code: Int): Int = code.takeIf { it in LESS..MORE } ?: EQUAL

    fun holds(
        code: Int,
        left: Int,
        right: Int,
    ): Boolean = holds(code, left.toLong(), right.toLong())
}
