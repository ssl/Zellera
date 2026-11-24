package zel.game.catalog.marketplace

import zel.game.catalog.CatalogServices
import zel.game.catalog.CatalogWallet
import zel.game.players.Player
import zel.net.clients.ClientConnection
import zel.protocol.outgoing.catalog.CatalogReplies
import zel.protocol.outgoing.catalog.MarketplaceSellStatus
import zel.protocol.outgoing.catalog.PurchaseError

/** With `marketplace.tokens_required` on, each offer spends a token; tokens are bought in batches for credits. */
class MarketplaceTokens(
    private val services: CatalogServices,
    private val store: MarketplaceTokenStore = MarketplaceTokenStore.jdbc { services.dataSource },
) {
    data class SellStatus(
        val status: MarketplaceSellStatus,
        val tokens: Int,
    )

    fun sellStatus(player: Player): SellStatus {
        val settings = services.settings
        if (!settings.marketplaceEnabled) return SellStatus(MarketplaceSellStatus.CLOSED, 0)
        MarketplaceStanding.refusal(player)?.let { return SellStatus(it, 0) }
        val held = held(player)
        return when {
            !settings.marketplaceTokensRequired || held > 0 -> SellStatus(MarketplaceSellStatus.OPEN, held)
            else -> SellStatus(MarketplaceSellStatus.NO_TOKENS, 0)
        }
    }

    fun mayOffer(
        player: Player,
        offers: Int,
    ): Boolean = !services.settings.marketplaceTokensRequired || held(player) >= offers

    fun spendForOffers(
        player: Player,
        offers: Int,
    ) {
        if (!services.settings.marketplaceTokensRequired || offers <= 0) return
        val spent =
            synchronized(player.stats) {
                val tally = player.stats.tally
                val spent = minOf(offers, tally.marketplaceTokens)
                tally.marketplaceTokens -= spent
                spent
            }
        if (spent > 0) store.add(player.profile.id, -spent)
    }

    /** Credits are given back when storing fails. */
    fun buyBatch(
        client: ClientConnection,
        player: Player,
    ): Boolean {
        val settings = services.settings
        if (!settings.marketplaceEnabled || !settings.marketplaceTokensRequired) return false
        if (!CatalogWallet.charge(player, settings.tokenBatchPrice, NO_POINTS, 0)) {
            CatalogReplies.notEnoughBalance(client, true, false, NO_POINTS)
            return false
        }
        if (!store.add(player.profile.id, settings.tokenBatchSize)) {
            CatalogWallet.refund(player, settings.tokenBatchPrice, NO_POINTS, 0)
            CatalogReplies.purchaseFailed(client, PurchaseError.GENERIC)
            return false
        }
        synchronized(player.stats) { player.stats.tally.marketplaceTokens += settings.tokenBatchSize }
        return true
    }

    private fun held(player: Player): Int = synchronized(player.stats) { player.stats.tally.marketplaceTokens }

    private companion object {
        const val NO_POINTS = 0
    }
}
