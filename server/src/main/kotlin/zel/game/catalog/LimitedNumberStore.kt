package zel.game.catalog

import zel.db.first
import zel.db.write
import java.sql.Connection
import java.sql.SQLException

/** A number has a row once it is sold; the primary key is what refuses selling it twice. */
internal class LimitedNumberStore(
    private val services: CatalogServices,
) {
    @Throws(SQLException::class)
    fun recordSale(
        connection: Connection,
        offerId: Int,
        number: Int,
        buyerId: Int,
        furniId: Int,
    ) {
        connection.write(SELL) {
            int(offerId)
            int(number)
            int(buyerId)
            int(services.now())
            int(furniId)
        }
        connection.write(COUNT_SALE) { int(offerId) }
    }

    /** Drops the sale of a number whose furni no longer exists; true when the number has no sale afterwards. */
    @Throws(SQLException::class)
    fun isFreeAgain(
        offerId: Int,
        number: Int,
    ): Boolean =
        services.database.dataSource.connection.use { connection ->
            val undone =
                connection.write(UNDO_ORPHANED) {
                    int(offerId)
                    int(number)
                }
            if (undone > 0) connection.write(UNCOUNT_SALE) { int(offerId) }
            connection.first(IS_SOLD, {
                int(offerId)
                int(number)
            }) { true } != true
        }

    private companion object {
        const val SELL =
            "INSERT INTO catalogue_limited_sales (offer_id, number, player_id, sold_at, furni_id) VALUES (?, ?, ?, ?, ?)"
        const val COUNT_SALE = "UPDATE catalogue_offers SET limited_sold = limited_sold + 1 WHERE id = ?"
        const val UNCOUNT_SALE =
            "UPDATE catalogue_offers SET limited_sold = limited_sold - 1 WHERE id = ? AND limited_sold > 0"
        const val UNDO_ORPHANED =
            "DELETE FROM catalogue_limited_sales WHERE offer_id = ? AND number = ?" +
                " AND NOT EXISTS (SELECT 1 FROM furni WHERE furni.id = catalogue_limited_sales.furni_id)"
        const val IS_SOLD = "SELECT 1 FROM catalogue_limited_sales WHERE offer_id = ? AND number = ?"
    }
}
