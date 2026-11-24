package zel.protocol.incoming.catalog

import zel.game.catalog.CatalogServices
import zel.game.catalog.TargetedOffer
import zel.net.clients.ClientConnection

internal class TargetOfferPurchase(
    private val services: CatalogServices,
) {
    @Suppress("ReturnCount") // each failed check drops the purchase
    fun purchase(
        client: ClientConnection,
        offerId: Int,
        requested: Int,
    ) {
        val player = client.player ?: return
        val stats = player.stats
        if (services.now() - stats.lastPurchaseTimestamp < services.settings.purchaseCooldown) return
        stats.lastPurchaseTimestamp = services.now().toLong()
        val catalog = services.catalog
        val offer = catalog.runningTargetedOffer()?.takeIf { it.id == offerId } ?: return
        // the record is opened when the offer is shown, so a player it was never shown to has none
        val record = stats.offerPurchaseOrNull(offerId) ?: return
        val item = catalog.offer(offer.catalogOfferId) ?: return
        val allowed = TargetedOffer.purchasableAmount(requested, offer.purchaseLimit, record.bought)
        // a limited edition sells one copy per order, so only one counts against the limit
        val units = if (item.isLimited) minOf(allowed, 1) else allowed
        if (units <= 0) return
        if (catalog.purchaseAt(item, player, units, offer.price)) record.record(units, services.now())
    }
}
