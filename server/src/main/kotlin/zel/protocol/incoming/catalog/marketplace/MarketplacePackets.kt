package zel.protocol.incoming.catalog.marketplace

import zel.game.catalog.CatalogServices
import zel.game.catalog.marketplace.MarketplaceTokens
import zel.net.clients.ClientConnection
import zel.protocol.generated.BuyMarketplaceOfferInCodec
import zel.protocol.generated.BuyMarketplaceTokensInCodec
import zel.protocol.generated.CancelAllMarketplaceOffersInCodec
import zel.protocol.generated.CancelMarketplaceOfferInCodec
import zel.protocol.generated.ClearMarketplaceOwnHistoryInCodec
import zel.protocol.generated.GetMarketplaceCanMakeOfferInCodec
import zel.protocol.generated.GetMarketplaceConfigurationInCodec
import zel.protocol.generated.GetMarketplaceItemStatsInCodec
import zel.protocol.generated.GetMarketplaceOffersInCodec
import zel.protocol.generated.GetMarketplaceOwnOffersInCodec
import zel.protocol.generated.MakeOfferInCodec
import zel.protocol.generated.MarketPlaceOffersOutCodec
import zel.protocol.generated.MarketPlaceOwnOffersOutCodec
import zel.protocol.generated.MarketplaceConfigurationOutCodec
import zel.protocol.generated.MarketplaceItemStatsOutCodec
import zel.protocol.generated.RedeemMarketplaceOfferCreditsInCodec
import zel.protocol.outgoing.catalog.CatalogMessages
import zel.protocol.outgoing.catalog.CatalogReplies
import zel.protocol.typed.ClientCooldown
import zel.protocol.typed.PacketPackage
import zel.protocol.typed.PacketRoute
import zel.protocol.typed.PacketSession
import zel.protocol.typed.player

private const val SEARCH_GAP_MS = 1000L
private const val ORDER_GAP_MS = 1000L
private const val ROW_GAP_MS = 200L

class MarketplacePackets(
    private val services: CatalogServices,
    sale: MarketplaceSale,
    private val tokens: MarketplaceTokens,
) : PacketPackage {
    override val name = "marketplace"

    /** Only a search that reads the database is paced; one answered from the cache costs nothing. */
    private val searchPace = ClientCooldown("marketplace.search", SEARCH_GAP_MS)

    /** What the client sends from a confirm window or a whole-list button: nobody confirms two a second. */
    private val orderPace = ClientCooldown("marketplace.order", ORDER_GAP_MS)

    /** Taking back is one click per row of the own list: faster than a hand goes down it is a script. */
    private val rowPace = ClientCooldown("marketplace.take_back", ROW_GAP_MS)

    override val routes =
        listOf(
            PacketRoute(GetMarketplaceConfigurationInCodec) { s, _ ->
                s.send(MarketplaceConfigurationOutCodec, CatalogMessages.marketplaceConfiguration(services.settings))
            },
            PacketRoute(GetMarketplaceOffersInCodec) { s, m ->
                sendOffers(s, OfferSearchInput.of(m.min, m.max, m.query, m.type), m.combineUniques == true)
            },
            PacketRoute(GetMarketplaceOwnOffersInCodec) { s, _ -> sendOwnOffers(s) },
            PacketRoute(GetMarketplaceCanMakeOfferInCodec) { s, _ -> sendSellStatus(s) },
            PacketRoute(BuyMarketplaceTokensInCodec) { s, _ ->
                if (orderPace.acquire(s.client) && tokens.buyBatch(s.client, s.player)) {
                    sendSellStatus(s)
                }
            },
            PacketRoute(GetMarketplaceItemStatsInCodec) { s, m ->
                if (m.int2 > 0) {
                    s.send(
                        MarketplaceItemStatsOutCodec,
                        CatalogMessages.marketplaceItemStats(services.marketplace, m.int1, m.int2),
                    )
                }
            },
            PacketRoute(MakeOfferInCodec) { s, m ->
                if (orderPace.acquire(s.client)) sale.handle(s.client, m)
            },
            PacketRoute(BuyMarketplaceOfferInCodec) { s, m ->
                if (orderPace.acquire(s.client) && m.offerId > 0) {
                    services.marketplace.buy(s.client, m.offerId)
                }
            },
            PacketRoute(CancelMarketplaceOfferInCodec) { s, m ->
                if (rowPace.acquire(s.client) && m.offerId > 0) {
                    val returned = services.marketplace.takeBack(requireNotNull(s.client.player), m.offerId)
                    CatalogReplies.marketplaceCancelResult(s.client, m.offerId, returned)
                }
            },
            PacketRoute(CancelAllMarketplaceOffersInCodec) { s, _ ->
                if (orderPace.acquire(s.client)) cancelAll(s)
            },
            PacketRoute(ClearMarketplaceOwnHistoryInCodec) { s, m ->
                if (orderPace.acquire(s.client)) clearHistory(s, m.state)
            },
            PacketRoute(RedeemMarketplaceOfferCreditsInCodec) { s, _ ->
                if (orderPace.acquire(s.client)) services.marketplace.collectSales(s.client)
            },
        )

    private fun sendOffers(
        s: PacketSession,
        search: OfferSearchInput,
        combineUniques: Boolean,
    ) {
        val cacheable = search.minPrice == -1 && search.maxPrice == -1 && search.query.isEmpty()
        val key = (search.sort shl 1) or (if (combineUniques) 1 else 0)
        val fee = services.settings.marketplaceFee
        val cached = if (cacheable) services.offerCache.under(fee)[key] else null
        if (cached == null && !searchPace.acquire(s.client)) return
        val message =
            cached
                ?: CatalogMessages
                    .marketplaceOffers(
                        services.marketplace.search(
                            search.minPrice,
                            search.maxPrice,
                            search.query,
                            search.sort,
                            combineUniques,
                        ),
                        fee,
                    ).also {
                        if (cacheable) services.offerCache[key] = it
                    }
        s.send(MarketPlaceOffersOutCodec, message)
    }

    private fun sendSellStatus(s: PacketSession) {
        val status = tokens.sellStatus(s.player)
        CatalogReplies.marketplaceSellStatus(s.client, status.status, status.tokens)
    }

    private fun sendOwnOffers(s: PacketSession) = sendOwnOffers(s.client)

    private fun sendOwnOffers(client: ClientConnection) {
        val listings = requireNotNull(client.player).inventory.listings
        val now = services.now()
        val offers = listings.all.toList()
        services.marketplace.closeExpired(offers, now)
        CatalogReplies.send(
            client,
            MarketPlaceOwnOffersOutCodec,
            CatalogMessages.marketplaceOwnOffers(
                offers,
                listings.uncollectedCredits,
                now,
                services.settings.marketplaceFee,
            ),
        )
    }

    private fun cancelAll(s: PacketSession) {
        if (!services.settings.marketplaceEnabled) {
            CatalogReplies.marketplaceCancelAllResult(s.client, emptyList(), false)
            return
        }
        val recalled = services.marketplace.takeBackOpen(requireNotNull(s.client.player))
        CatalogReplies.marketplaceCancelAllResult(s.client, recalled, true)
        sendOwnOffers(s.client)
    }

    private fun clearHistory(
        s: PacketSession,
        state: Int,
    ) {
        if (!services.settings.marketplaceEnabled) {
            CatalogReplies.marketplaceClearHistoryResult(s.client, false)
            return
        }
        val cleared = services.marketplace.clearTab(s.client, state)
        CatalogReplies.marketplaceClearHistoryResult(s.client, cleared)
        if (cleared) sendOwnOffers(s.client)
    }
}
