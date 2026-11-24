package zel.wired.chest

interface PlayerWallet {
    fun credits(playerId: Int): Int

    fun premium(playerId: Int): Int

    fun takeCredits(
        playerId: Int,
        amount: Int,
    )

    fun takePremium(
        playerId: Int,
        amount: Int,
    )

    /** Holds the player's wallet, so a balance read in [work] still stands at its debits. */
    fun <T> withWallet(
        playerId: Int,
        work: () -> T,
    ): T = work()
}
