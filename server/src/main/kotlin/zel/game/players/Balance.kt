package zel.game.players

/** Never below zero, never above the balance column's maximum. */
@JvmInline
value class Balance private constructor(
    val amount: Int,
) {
    operator fun plus(delta: Int): Balance =
        requireNotNull(plusOrNull(delta)) { "a balance of $amount cannot take $delta" }

    fun plusOrNull(delta: Int): Balance? = (amount.toLong() + delta).takeIf { it in RANGE }?.let { Balance(it.toInt()) }

    companion object {
        private val RANGE = 0L..Int.MAX_VALUE.toLong()

        fun of(amount: Int): Balance {
            require(amount >= 0) { "a balance is never negative: $amount" }
            return Balance(amount)
        }
    }
}
