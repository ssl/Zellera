package zel.game.games.snowball

import zel.core.rules.FreeGames
import zel.core.rules.GameTokenOffer
import zel.core.rules.rules
import zel.game.economy.EconomyLedger
import zel.game.economy.LedgerEntry
import zel.game.players.Player
import zel.protocol.outgoing.catalog.CatalogMessages
import zel.protocol.outgoing.catalog.CatalogReplies
import zel.protocol.outgoing.catalog.PurchaseError
import zel.protocol.outgoing.users.UserCredits
import zel.protocol.outgoing.users.UserCurrency

/** Offer ids are the offers' places in the list, from 1. */
class SnowballTokens(
    private val store: SnowballPlayStore,
) {
    /** Tokens are sold only where they are spent: with a daily allowance of free games, while the game is open. */
    fun offers(): List<GameTokenOffer> =
        if (rules.snowball && rules.snowballFreeGames == FreeGames.DAILY) rules.snowballTokenOffers else emptyList()

    /** The price is taken whole or not at all; false when nothing was sold. */
    fun buy(
        player: Player,
        offerId: Int,
    ): Boolean {
        val client = player.client ?: return false
        val offer = offers().getOrNull(offerId - 1)
        if (offer == null) {
            CatalogReplies.purchaseFailed(client, PurchaseError.GENERIC)
            return false
        }
        val wallet = player.wallet
        if (offer.credits > 0 && !wallet.takeCredits(offer.credits)) {
            CatalogReplies.notEnoughBalance(client, true, false, 0)
            return false
        }
        val operation = LedgerEntry.newKey("snowball-tokens")
        if (offer.premium > 0 &&
            !wallet.takePoints(EconomyLedger.PREMIUM, offer.premium, REASON, operation, null)
        ) {
            if (offer.credits > 0) wallet.changeCredits(offer.credits, REASON)
            CatalogReplies.notEnoughBalance(client, false, true, EconomyLedger.PREMIUM)
            return false
        }
        if (!store.addTokens(player.profile.id, offer.games)) {
            if (offer.credits > 0) wallet.changeCredits(offer.credits, REASON)
            if (offer.premium > 0) wallet.changePoints(EconomyLedger.PREMIUM, offer.premium, REASON)
            CatalogReplies.purchaseFailed(client, PurchaseError.GENERIC)
            return false
        }
        CatalogReplies.purchaseOk(client, CatalogMessages.purchaseOk(null))
        client.send(UserCredits(player))
        client.send(UserCurrency(player))
        return true
    }

    private companion object {
        const val REASON = "snowball.tokens"
    }
}
