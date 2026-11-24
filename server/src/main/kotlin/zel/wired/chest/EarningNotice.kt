package zel.wired.chest

interface EarningNotice {
    /** Tells [playerId] that credits wait in the earnings centre, before the answer to the move that put them there. */
    fun announceEarning(
        playerId: Int,
        category: Int,
    ) = Unit
}
