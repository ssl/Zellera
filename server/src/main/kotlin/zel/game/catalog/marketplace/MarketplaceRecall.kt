package zel.game.catalog.marketplace

import org.slf4j.LoggerFactory
import zel.db.allOrNothing
import zel.db.orFallback
import zel.game.catalog.CatalogServices
import zel.game.players.Player
import zel.protocol.generated.FurniListInvalidateOut
import zel.protocol.generated.FurniListInvalidateOutCodec
import zel.protocol.outgoing.inventory.AddRoomItem
import java.sql.Connection
import java.sql.SQLException

internal class MarketplaceRecall(
    private val services: CatalogServices,
) {
    fun takeBack(
        player: Player,
        offerId: Int,
    ): Boolean {
        val listings = player.inventory.listings
        val listing = listings.find(offerId) ?: return false
        val sellerId = player.profile.id
        val returned =
            log.orFallback("taking back marketplace offer $offerId of player $sellerId", false) {
                services.database.dataSource.connection
                    .use { commit(it, offerId, listing.furniId, sellerId) }
            }
        if (!returned) return false
        listings.remove(listing)
        services.offerCache.clear()
        restore(player, listing.furniId)
        return true
    }

    private fun restore(
        player: Player,
        furniId: Int,
    ) {
        val furni = services.items.loadFurni(furniId) ?: return
        player.inventory.furni.put(furni)
        player.client?.let { client ->
            client.send(AddRoomItem(furni))
            client.send(FurniListInvalidateOutCodec, FurniListInvalidateOut)
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(MarketplaceRecall::class.java)

        @Throws(SQLException::class)
        fun commit(
            connection: Connection,
            offerId: Int,
            furniId: Int,
            sellerId: Int,
        ): Boolean {
            if (offerId <= 0 || furniId <= 0 || sellerId <= 0) return false
            return connection.allOrNothing {
                (
                    MarketplaceStore.deleteOffer(connection, offerId, sellerId) &&
                        MarketplaceStore.handOver(connection, furniId, sellerId)
                ).takeIf { it }
            } ?: false
        }
    }
}
