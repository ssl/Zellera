package zel.game.catalog.marketplace

import org.slf4j.LoggerFactory
import zel.db.orFallback
import zel.game.catalog.CatalogServices
import zel.game.items.OwnedFurni
import zel.game.players.Player
import zel.net.clients.ClientConnection

class Marketplace internal constructor(
    private val services: CatalogServices,
    settlement: SaleSettlement,
) {
    private val reads = MarketplaceReads(services)
    private val selling = MarketplaceSelling(services)
    private val checkout = MarketplaceCheckout(services, reads, settlement)
    private val payout = MarketplacePayout(services)
    private val recall = MarketplaceRecall(services)

    val stats = MarketplaceStats(services)
    private val log = LoggerFactory.getLogger(Marketplace::class.java)

    val enabled: Boolean get() = services.settings.marketplaceEnabled

    fun soldCreditsGoToEarnings(): Boolean = services.settings.soldCreditsGoToEarnings()

    val fee: MarketplaceFee get() = services.settings.marketplaceFee

    fun ownListings(player: Player): Collection<MarketplaceListing> = reads.listingsOf(player.profile.id)

    fun list(
        client: ClientConnection,
        furni: OwnedFurni,
        price: Int,
    ): Boolean = selling.list(client, furni, price)

    fun buy(
        client: ClientConnection,
        offerId: Int,
    ) = checkout.buy(client, offerId)

    fun collectSales(client: ClientConnection) {
        client.player?.let(payout::collect)
    }

    fun takeBack(
        player: Player,
        offerId: Int,
    ): Boolean = recall.takeBack(player, offerId)

    /** The rows change off the packet thread. */
    fun closeExpired(
        listings: Collection<MarketplaceListing>,
        now: Int,
    ) {
        val expired =
            listings.filter {
                it.state == ListingState.OPEN && !MarketplacePrices.isOpen(it.listedAt, now)
            }
        if (expired.isEmpty()) return
        expired.forEach { it.state = ListingState.CLOSED }
        services.threading.run {
            log.orFallback("closing ${expired.size} expired marketplace offers", Unit) {
                services.database.dataSource.connection.use { connection ->
                    expired.forEach { MarketplaceStore.close(connection, it.offerId) }
                }
            }
        }
    }

    fun takeBackOpen(player: Player): List<Int> =
        player.inventory.listings.all
            .filter { it.state == ListingState.OPEN }
            .filter { recall.takeBack(player, it.offerId) }
            .map { it.offerId }

    fun clearTab(
        client: ClientConnection,
        state: Int,
    ): Boolean {
        val player = client.player ?: return false
        val listings =
            player.inventory.listings.all
                .filter { it.state.code == state }
        if (listings.isEmpty()) return false
        if (state == ListingState.SOLD.code) {
            payout.collect(player)
            return true
        }
        return listings.map { recall.takeBack(player, it.offerId) }.any { it }
    }

    fun search(
        minPrice: Int,
        maxPrice: Int,
        query: String,
        sort: Int,
        combineUniques: Boolean,
    ): ListingPage = reads.browse(MarketplaceSearch.ListingFilter(minPrice, maxPrice, query, sort, combineUniques))
}
