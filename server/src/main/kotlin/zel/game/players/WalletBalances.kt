package zel.game.players

import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap

internal class WalletBalances(
    credits: Int,
) {
    private val lock = Any()
    private var creditBalance = Balance.of(credits)
    private val currencies = Int2IntOpenHashMap()

    fun restoreCurrency(
        type: Int,
        amount: Int,
    ) = synchronized(lock) { currencies.put(type, maxOf(0, amount)) }

    fun credits(): Int = synchronized(lock) { creditBalance.amount }

    fun currency(type: Int): Int = synchronized(lock) { currencies.get(type) }

    fun replaceCredits(amount: Int) = synchronized(lock) { creditBalance = Balance.of(amount) }

    fun setCurrency(
        type: Int,
        amount: Int,
    ) = synchronized(lock) { currencies.put(type, Balance.of(amount).amount) }
}
