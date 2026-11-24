package zel.game.catalog

import zel.db.rows
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.sql.Connection
import java.sql.SQLException

class LimitedEditions(
    private val services: CatalogServices,
    private val data: CatalogData,
) {
    fun get(offer: CatalogOffer): LimitedEditionStock? = data.limitedNumbers[offer.id]

    /** What is free is every number up to the edition's size that has no sale. */
    fun load() {
        data.limitedNumbers.clear()
        try {
            services.database.dataSource.connection.use { connection ->
                val sold = soldNumbers(connection)
                editions(connection).forEach { edition ->
                    val taken = sold[edition.offerId].orEmpty()
                    val free = (1..edition.size).filterNot(taken::contains)
                    data.limitedNumbers[edition.offerId] =
                        LimitedEditionStock(edition.offerId, free, edition.size, edition.order, services)
                }
            }
        } catch (e: SQLException) {
            log.error("Loading the limited editions failed", e)
        }
    }

    @Synchronized
    fun createOrUpdate(offer: CatalogOffer): LimitedEditionStock? {
        if (!offer.isLimited) return null
        val stock =
            data.limitedNumbers.getOrPut(offer.id) {
                val all = (1..offer.editionSize).toList()
                LimitedEditionStock(offer.id, all, offer.editionSize, offer.editionOrder, services)
            }
        stock.growTo(offer.editionSize)
        return stock
    }

    private class Edition(
        val offerId: Int,
        val size: Int,
        val order: LimitedOrder,
    )

    private fun editions(connection: Connection): List<Edition> =
        connection.rows(EDITIONS) {
            Edition(it.getInt("id"), it.getInt("limited_total"), LimitedOrder.of(it.getString("limited_order")))
        }

    private fun soldNumbers(connection: Connection): Map<Int, Set<Int>> =
        connection
            .rows(SOLD) { it.getInt("offer_id") to it.getInt("number") }
            .groupBy({ it.first }, { it.second })
            .mapValues { (_, numbers) -> numbers.toHashSet() }

    private companion object {
        val log: Logger = LoggerFactory.getLogger(LimitedEditions::class.java)
        const val EDITIONS = "SELECT id, limited_total, limited_order FROM catalogue_offers WHERE limited_total > 0"
        const val SOLD = "SELECT offer_id, number FROM catalogue_limited_sales"
    }
}
