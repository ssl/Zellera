package zel.game.earnings

import java.sql.Connection
import java.sql.SQLException

/** Stored in the chest move's transaction, announced after it commits. */
class WiredCreditEarnings(
    private val earnings: IncomeRewards,
) {
    fun announce(
        userId: Int,
        category: Int,
    ) = earnings.announce(userId, category, IncomeRewards.Notice.UNSEEN)

    companion object {
        /** The payout goes to the wallet, not to the earnings centre. */
        const val PURSE = -1

        /** Credits over a coin chest's capacity go to the chest's owner. */
        const val CHEST_OVERFLOW = IncomeRewards.WIRED_CHEST

        fun categoryOf(storedCategory: Int): Int =
            when (storedCategory) {
                PURSE -> PURSE
                IncomeRewards.AGENCY -> IncomeRewards.AGENCY
                else -> IncomeRewards.GAMES
            }

        @Throws(SQLException::class)
        fun store(
            connection: Connection,
            userId: Int,
            category: Int,
            amount: Int,
            createdAt: Int,
        ) = JdbcIncomeRewardStore.insert(connection, userId, category, IncomeRewards.CREDITS, amount, "", createdAt)
    }
}
