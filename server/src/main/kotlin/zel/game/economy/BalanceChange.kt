package zel.game.economy

/** [replayed]: the entry was posted before, and this is the balance it left then. */
data class BalanceChange(
    val after: Int,
    val amount: Int,
    val replayed: Boolean,
) {
    val before: Int get() = after - amount
}
