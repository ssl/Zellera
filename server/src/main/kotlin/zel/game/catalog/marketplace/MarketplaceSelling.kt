package zel.game.catalog.marketplace

import zel.db.allOrNothing
import zel.game.catalog.CatalogServices
import zel.game.items.OwnedFurni
import zel.net.clients.ClientConnection
import zel.protocol.generated.FurniListInvalidateOut
import zel.protocol.generated.FurniListInvalidateOutCodec
import zel.protocol.generated.FurniListRemoveOut
import zel.protocol.generated.FurniListRemoveOutCodec
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.sql.SQLException

internal class MarketplaceSelling(
    private val services: CatalogServices,
) {
    fun list(
        client: ClientConnection,
        furni: OwnedFurni,
        price: Int,
    ): Boolean {
        if (!MarketplacePrices.isListable(price)) return false
        val player = client.player ?: return false
        val hand = player.inventory.furni
        if (!hand.takeAll(listOf(furni))) return false
        val listing = putUpForSale(player.profile.id, furni, price)
        if (listing == null) {
            hand.putBack(listOf(furni))
            return false
        }
        player.inventory.listings.add(listing)
        services.offerCache.clear()
        client.send(FurniListRemoveOutCodec, FurniListRemoveOut(furni.stripId))
        client.send(FurniListInvalidateOutCodec, FurniListInvalidateOut)
        return true
    }

    private fun putUpForSale(
        playerId: Int,
        furni: OwnedFurni,
        price: Int,
    ): MarketplaceListing? {
        val listedAt = services.now()
        return try {
            services.database.dataSource.connection.use { connection ->
                connection.allOrNothing {
                    if (!MarketplaceStore.escrow(connection, furni.id, playerId)) return@allOrNothing null
                    MarketplaceStore.open(connection, furni.id, playerId, price, listedAt)?.let { offerId ->
                        MarketplaceListing.opened(offerId, furni, playerId, price, listedAt)
                    }
                }
            }
        } catch (failure: SQLException) {
            log.error("furni {} of player {} was not listed", furni.id, playerId, failure)
            null
        }
    }

    private companion object {
        val log: Logger = LoggerFactory.getLogger(MarketplaceSelling::class.java)
    }
}
