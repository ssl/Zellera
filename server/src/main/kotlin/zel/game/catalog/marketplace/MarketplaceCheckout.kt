package zel.game.catalog.marketplace

import zel.game.achievements.AchievementEvents
import zel.game.catalog.CatalogServices
import zel.game.earnings.IncomeRewards
import zel.game.items.OwnedFurni
import zel.game.players.Player
import zel.game.players.WalletWrites
import zel.net.clients.ClientConnection
import zel.protocol.generated.ActivityPointNotificationOut
import zel.protocol.generated.ActivityPointNotificationOutCodec
import zel.protocol.generated.FurniListInvalidateOut
import zel.protocol.generated.FurniListInvalidateOutCodec
import zel.protocol.outgoing.catalog.CatalogReplies
import zel.protocol.outgoing.catalog.MarketplaceBuyResult
import zel.protocol.outgoing.inventory.AddRoomItem
import zel.protocol.outgoing.users.UserCredits

internal class MarketplaceCheckout(
    private val services: CatalogServices,
    private val reads: MarketplaceReads,
    private val settlement: SaleSettlement,
) {
    private val currency get() = MarketplaceCurrency(services.settings.marketplaceCurrency)
    private val fee get() = services.settings.marketplaceFee

    /**
     * Cheapest checks first: the listing, the buyer's purse, then the furni row. The buyer's own offer (it would only
     * burn the commission) and one whose seller lost the trade pass or is trade locked answer as gone. A buyer without
     * the trade pass or under a trade lock is refused first, with the sell status that says why.
     */
    @Suppress("ReturnCount") // one early answer per check
    fun buy(
        client: ClientConnection,
        offerId: Int,
    ) {
        val buyer = client.player ?: return
        if (!services.settings.marketplaceEnabled) {
            return CatalogReplies.marketplaceBuyResult(client, MarketplaceBuyResult.GONE, 0, 0, offerId)
        }
        MarketplaceStanding.refusal(buyer)?.let { return CatalogReplies.marketplaceSellStatus(client, it, 0) }
        val offer = reads.listing(offerId)
        if (offer == null || offer.sellerId == buyer.profile.id || !sellerMayTrade(offer)) {
            return CatalogReplies.marketplaceBuyResult(client, MarketplaceBuyResult.GONE, 0, 0, offerId)
        }
        if (!offer.isForSale(services.now())) return answerGone(client, offer.definitionId, offerId)
        val charge = fee.buyerPays(offer.price)
        if (!currency.canPay(buyer, charge)) {
            return CatalogReplies.marketplaceBuyResult(client, MarketplaceBuyResult.NO_CREDITS, 0, charge, offerId)
        }
        val furni = services.items.loadFurni(offer.furniId) ?: return answerGone(client, offer.definitionId, offerId)
        complete(client, buyer, offerId, offer, furni, charge)
    }

    private fun sellerMayTrade(offer: MarketplaceReads.Listing): Boolean =
        services
            .clients()
            ?.onlinePlayer(offer.sellerId)
            ?.stats
            ?.standing
            ?.mayTrade()
            ?: offer.sellerMayTradeAt(services.now())

    private fun complete(
        client: ClientConnection,
        buyer: Player,
        offerId: Int,
        offer: MarketplaceReads.Listing,
        furni: OwnedFurni,
        charge: Int,
    ) {
        val soldAt = services.now()
        val earnings = if (services.settings.soldCreditsGoToEarnings()) fee.sellerGets(offer.price) else 0
        val payoutCurrency = currency.ledgerType.takeUnless { services.settings.soldCreditsGoToEarnings() }
        val sale =
            SaleSettlement.Sale(
                offerId,
                furni.id,
                buyer.profile.id,
                currency.ledgerType,
                charge,
                soldAt,
                offer.sellerId,
                earnings,
                payoutCurrency,
            )
        val committed =
            WalletWrites.locked(buyer) {
                val paid = settlement.settle(sale)
                paid?.let { WalletWrites.publish(buyer, sale.ledgerCurrency, it.after) }
                paid != null
            }
        if (!committed) {
            answerGone(client, furni.definition.id, offerId)
            return
        }

        services.offerCache.clear()
        hand(furni, buyer)
        showPurchase(client, buyer, furni, charge, offerId)
        tellSeller(offer.sellerId, offerId, soldAt, earnings, payoutCurrency)
    }

    private fun hand(
        furni: OwnedFurni,
        buyer: Player,
    ) {
        furni.userId = buyer.profile.id
        furni.markChanged()
        services.threading.run(furni)
        buyer.inventory.furni.put(furni)
    }

    private fun showPurchase(
        client: ClientConnection,
        buyer: Player,
        furni: OwnedFurni,
        charge: Int,
        offerId: Int,
    ) {
        if (currency.isCredits) {
            client.send(UserCredits(buyer))
        } else {
            client.send(
                ActivityPointNotificationOutCodec,
                ActivityPointNotificationOut(currency.balanceOf(buyer), -charge, currency.ledgerType),
            )
        }
        client.send(AddRoomItem(furni))
        client.send(FurniListInvalidateOutCodec, FurniListInvalidateOut)
        CatalogReplies.marketplaceBuyResult(client, MarketplaceBuyResult.BOUGHT, 0, charge, offerId)
    }

    private fun tellSeller(
        sellerId: Int,
        offerId: Int,
        soldAt: Int,
        earnings: Int,
        payoutCurrency: Int?,
    ) {
        services.clients()?.onlinePlayer(sellerId)?.inventory?.listings?.find(offerId)?.let {
            it.state = ListingState.SOLD
            it.soldAt = soldAt
            it.payoutCurrency = payoutCurrency
        }
        if (earnings > 0) services.incomeRewards.notifyNew(sellerId, IncomeRewards.MARKETPLACE)
        AchievementEvents.explore().marketplaceItemSold(sellerId)
    }

    private fun answerGone(
        client: ClientConnection,
        definitionId: Int,
        offerId: Int,
    ) {
        val cheapest = reads.cheapestOpen(definitionId)
        if (cheapest == null) {
            CatalogReplies.marketplaceBuyResult(client, MarketplaceBuyResult.GONE, 0, 0, offerId)
        } else {
            CatalogReplies.marketplaceBuyResult(
                client,
                MarketplaceBuyResult.OFFER_CHANGED,
                cheapest.offerId,
                fee.buyerPays(cheapest.price),
                offerId,
            )
        }
    }
}
