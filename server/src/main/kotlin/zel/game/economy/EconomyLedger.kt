package zel.game.economy

import zel.core.rules.rules
import java.sql.SQLException
import javax.sql.DataSource

class EconomyLedger(
    private val dataSource: DataSource,
) {
    @Throws(SQLException::class)
    fun post(entry: LedgerEntry): BalanceChange = postAll(listOf(entry)).first()

    @Throws(SQLException::class)
    fun postAll(entries: List<LedgerEntry>): List<BalanceChange> {
        LedgerTransaction.requireOnePlayer(entries)
        return dataSource.connection.use { connection ->
            connection.autoCommit = false
            runCatching { LedgerTransaction(connection).postAll(entries).also { connection.commit() } }
                .getOrElse { failure ->
                    runCatching { connection.rollback() }.exceptionOrNull()?.let(failure::addSuppressed)
                    throw failure
                }
        }
    }

    companion object {
        const val CREDITS = -1
        val POINTS: Int get() = Currencies.id(CurrencyRole.POINTS)
        val PREMIUM: Int get() = Currencies.id(CurrencyRole.PREMIUM)

        /** The activity points a player holds: the content's, and the seasonal ones while the rules turn them on. */
        fun pointTypes(): List<Int> {
            val seasonal = if (rules.seasonalCurrency) rules.seasonalCurrencyTypes else emptyList()
            return (Currencies.pointIds() + seasonal).distinct()
        }
    }
}
