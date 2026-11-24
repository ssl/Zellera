package zel.game.catalog

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import zel.db.allOrNothing
import zel.db.insertForId
import zel.db.orFallback
import zel.db.writeEach
import javax.sql.DataSource

class ShopPurchaseRecord(
    private val boughtAt: Int,
    private val playerId: Int,
    private val offerId: Int,
    private val furniIds: Collection<Int>,
    private val offerName: String,
    private val priceCredits: Int,
    private val pricePoints: Int,
    private val pointsType: Int,
    private val amount: Int,
) {
    fun store(dataSource: DataSource): Boolean =
        log.orFallback("purchase log of offer $offerId for player $playerId", false) {
            dataSource.connection.use { connection ->
                connection.allOrNothing {
                    connection
                        .insertForId(INSERT_PURCHASE) {
                            int(boughtAt)
                            int(playerId)
                            intOrNullWhenNotPositive(offerId)
                            string(offerName)
                            int(priceCredits)
                            int(pricePoints)
                            int(pointsType)
                            int(amount)
                        }?.also { purchaseId ->
                            connection.writeEach(INSERT_FURNI, furniIds.distinct()) { furniId ->
                                int(purchaseId)
                                int(furniId)
                            }
                        }
                } != null
            }
        }

    private companion object {
        val log: Logger = LoggerFactory.getLogger(ShopPurchaseRecord::class.java)
        const val INSERT_PURCHASE =
            "INSERT INTO `purchases` (bought_at, player_id, offer_id, offer_name, " +
                "price_credits, price_currency, currency, amount) VALUES (?, ?, ?, ?, ?, ?, ?, ?)"
        const val INSERT_FURNI = "INSERT INTO `purchase_furni` (purchase_id, furni_id) VALUES (?, ?)"
    }
}
