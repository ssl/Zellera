package zel.protocol.outgoing.catalog

import zel.game.catalog.BuildPassBuyPages
import zel.game.catalog.BundleDiscount
import zel.game.catalog.Catalog
import zel.game.catalog.CatalogMode
import zel.game.catalog.CatalogOffer
import zel.game.catalog.CatalogPage
import zel.game.catalog.CatalogSettings
import zel.game.catalog.MembershipOffer
import zel.game.catalog.NewAdditions
import zel.game.catalog.OfferPrice
import zel.game.catalog.OfferWire
import zel.game.catalog.RecyclerPrizes
import zel.game.catalog.TargetedOffer
import zel.game.catalog.layouts.PageForm
import zel.game.catalog.marketplace.ListingPage
import zel.game.catalog.marketplace.ListingState
import zel.game.catalog.marketplace.Marketplace
import zel.game.catalog.marketplace.MarketplaceFee
import zel.game.catalog.marketplace.MarketplaceListing
import zel.game.catalog.marketplace.MarketplacePrices
import zel.game.pets.MonsterPlant
import zel.game.pets.Pet
import zel.game.pets.PetPalette
import zel.game.players.Player
import zel.game.players.PlayerOfferPurchase
import zel.protocol.generated.BuildPassFurniCountOut
import zel.protocol.generated.BuildPassPlacementWarningOut
import zel.protocol.generated.BuildPassSubscriptionStatusOut
import zel.protocol.generated.BundleDiscountRulesetOut
import zel.protocol.generated.CatalogIndexOut
import zel.protocol.generated.CatalogPageOut
import zel.protocol.generated.CatalogPageOutCodec
import zel.protocol.generated.CatalogPageWithEarliestExpiryOut
import zel.protocol.generated.ClubGiftInfoOut
import zel.protocol.generated.ClubGiftInfoOutCodec
import zel.protocol.generated.ClubOffersOut
import zel.protocol.generated.ClubOffersOutCodec
import zel.protocol.generated.GiftWrappingConfigurationOut
import zel.protocol.generated.MarketPlaceOffersOut
import zel.protocol.generated.MarketPlaceOwnOffersOut
import zel.protocol.generated.MarketplaceConfigurationOut
import zel.protocol.generated.MarketplaceItemStatsOut
import zel.protocol.generated.MarketplaceItemStatsOutCodec
import zel.protocol.generated.Node
import zel.protocol.generated.PacketReader
import zel.protocol.generated.PacketWriter
import zel.protocol.generated.PetReceivedOut
import zel.protocol.generated.ProductOfferOut
import zel.protocol.generated.ProductOfferOutCodec
import zel.protocol.generated.PurchaseOKOut
import zel.protocol.generated.PurchaseOKOutCodec
import zel.protocol.generated.RecyclerPrizesOut
import zel.protocol.generated.SeasonalCalendarDailyOfferOut
import zel.protocol.generated.SeasonalCalendarDailyOfferOutCodec
import zel.protocol.generated.SellablePetPalettesOut
import zel.protocol.generated.TargetedOfferOut
import zel.util.MILLIS_PER_SECOND
import zel.util.SECONDS_PER_MINUTE
import it.unimi.dsi.fastutil.ints.IntArrayList
import it.unimi.dsi.fastutil.ints.IntOpenHashSet
import org.slf4j.LoggerFactory
import java.util.function.IntUnaryOperator

@Suppress("TooManyFunctions") // one builder per message
object CatalogMessages {
    private val log = LoggerFactory.getLogger(CatalogMessages::class.java)

    private const val MAX_OFFERS = 4000
    private const val MAX_CHILDREN = 500
    private const val MAX_DEPTH = 20
    private const val MARKETPLACE_LIMITED = 3
    private const val MARKETPLACE_WALL = 2
    private const val PET_PART_BYTES = 12

    /** Every monthly club gift is a VIP gift. */
    private const val VIP_GIFT = true

    fun catalogIndex(
        catalog: Catalog,
        player: Player,
        mode: String,
    ): CatalogIndexOut? =
        try {
            val type = CatalogMode.fromCode(mode)
            val tree = catalog.visibleChildren(player, type)
            val pages = tree[-1].orEmpty()
            CatalogIndexOut(
                boolean1 = true,
                int1 = 0,
                int2 = -1,
                string1 = "root",
                string2 = "",
                list1 = emptyList(),
                list2 =
                    pages.take(MAX_CHILDREN).map {
                        CatalogIndexOut.List2Item(node(tree, it, 1))
                    },
                boolean2 = NewAdditions.waiting(catalog, player.stats.tally.newAdditionsSeen),
                string3 = mode,
            )
        } catch (
            @Suppress("TooGenericExceptionCaught") e: RuntimeException, // a broken page must not stop the index
        ) {
            log.error("Caught exception", e)
            null
        }

    private fun node(
        tree: Map<Int, List<CatalogPage>>,
        category: CatalogPage,
        depth: Int,
    ): Node {
        val children = tree[category.id].orEmpty()
        val seen = IntOpenHashSet()
        val offerIds = IntArrayList()
        category.offerIds.forEach { id -> if (id > 0 && seen.add(id)) offerIds.add(id) }
        if (offerIds.size > MAX_OFFERS) {
            log.warn(
                "catalog: page {} lists {} offers, the index packet carries the first {}",
                category.id,
                offerIds.size,
                MAX_OFFERS,
            )
        }
        return Node(
            boolean1 = category.info.visible,
            int1 = category.info.icon,
            int2 = if (category.isFolder) -1 else category.id,
            string1 = category.info.code,
            string2 = category.info.caption,
            list1 = offerIds.take(MAX_OFFERS),
            list2 =
                if (depth >= MAX_DEPTH) {
                    emptyList()
                } else {
                    children.take(MAX_CHILDREN).map { Node.List2Item(node(tree, it, depth + 1)) }
                },
        )
    }

    fun catalogPage(
        catalog: Catalog,
        page: CatalogPage,
        offerId: Int,
        mode: String,
    ): CatalogPageOut {
        val items = catalog.offersShown(page)
        val buyPage = BuildPassBuyPages.isBuyPage(page)
        val featured = if (page.form.listsFeaturedPages) catalog.contents.featuredPages() else null
        val body =
            bytesOf { m ->
                m.int(page.id)
                m.string(mode)
                page.form.write(m, page)
                m.int(items.size)
                items.forEach { item ->
                    if (buyPage) {
                        val clubOffer = catalog.membershipOffers.forPage(page, item.id)
                        BuildPassBuyPages.serializeOffer(m, item, clubOffer, OfferWire.Form.PAGE)
                    } else {
                        item.writeTo(m)
                    }
                }
                m.int(offerId)
                m.boolean(false)
                if (featured != null) {
                    m.int(featured.size)
                    featured.forEach { it.writeTo(m) }
                }
            }
        return CatalogPageOutCodec.decodeBody(body)
    }

    fun productOffer(item: CatalogOffer): ProductOfferOut = ProductOfferOutCodec.decodeBody(bytesOf(item::writeTo))

    fun calendarDailyOffer(item: CatalogOffer): SeasonalCalendarDailyOfferOut =
        SeasonalCalendarDailyOfferOutCodec.decodeBody(
            bytesOf {
                it.int(item.pageId)
                item.writeTo(it)
            },
        )

    /** An empty name hides the countdown. */
    fun pageEndingFirst(page: CatalogPage?): CatalogPageWithEarliestExpiryOut =
        CatalogPageWithEarliestExpiryOut(
            page?.info?.code.orEmpty(),
            page?.secondsToEnd ?: 0,
            page?.info?.expiryImage.orEmpty(),
        )

    fun purchaseOk(item: CatalogOffer?): PurchaseOKOut = purchaseOkFrom(item?.let { bytesOf(it::writePurchased) })

    fun purchaseOkClubPage(
        pageOffer: CatalogOffer,
        clubOffer: MembershipOffer?,
    ): PurchaseOKOut =
        purchaseOkFrom(
            bytesOf { BuildPassBuyPages.serializeOffer(it, pageOffer, clubOffer, OfferWire.Form.PURCHASE) },
        )

    fun purchaseOkClubOffer(offer: MembershipOffer): PurchaseOKOut =
        purchaseOkOffer(
            PurchasedOffer(
                offer.id,
                offer.name.orEmpty(),
                offer.price.credits,
                offer.price.points,
                offer.price.pointsType,
                "",
                emptyList(),
            ),
        )

    fun purchaseOkOffer(offer: PurchasedOffer): PurchaseOKOut =
        purchaseOkFrom(
            bytesOf { m ->
                val price = OfferPrice(offer.credits, offer.points, offer.pointsType)
                OfferWire.writeHead(m, offer.offerId, offer.localizationId, price, true, OfferWire.Form.PURCHASE)
                m.int(offer.productParams.size)
                offer.productParams.forEach { param ->
                    m.string(offer.productType)
                    m.int(0)
                    m.string(param)
                    m.int(1)
                    m.boolean(false)
                }
                OfferWire.writeTail(m, offer.clubLevel, false, "", OfferWire.Form.PURCHASE)
            },
        )

    private fun purchaseOkFrom(body: ByteArray?): PurchaseOKOut =
        body?.let(PurchaseOKOutCodec::decodeBody) ?: EMPTY_PURCHASE_OK

    private val EMPTY_PURCHASE_OK =
        PurchaseOKOut(
            int1 = 0,
            string1 = "",
            boolean1 = false,
            int2 = 0,
            int3 = 0,
            int4 = 0,
            boolean2 = true,
            list1 =
                listOf(
                    PurchaseOKOut.List1Item(
                        "s",
                        PurchaseOKOut.List1Item.Branch1.Else(
                            0,
                            "",
                            1,
                            false,
                            PurchaseOKOut.List1Item.Branch1.Else.Branch1.Else,
                        ),
                    ),
                ),
            int5 = 0,
            boolean3 = false,
        )

    fun clubGifts(
        catalog: Catalog,
        daysTillNextGift: Int,
        availableGifts: Int,
        daysAsClub: Int,
    ): ClubGiftInfoOut {
        val page = catalog.pageWithRole(PageForm.Role.CLUB_GIFT)
        val items =
            page
                ?.offers
                ?.values
                ?.sortedWith(catalog.settings.itemOrder())
                .orEmpty()
        val body =
            bytesOf { m ->
                m.int(daysTillNextGift)
                m.int(availableGifts)
                if (page == null) {
                    m.int(0)
                    m.int(0)
                    return@bytesOf
                }
                m.int(items.size)
                items.forEach { it.writeTo(m) }
                m.int(items.size)
                items.forEach { item ->
                    val daysRequired = item.presetState.toIntOrNull() ?: 0
                    m.int(item.id)
                    m.boolean(VIP_GIFT)
                    m.int(daysRequired)
                    m.boolean(daysRequired <= daysAsClub)
                }
            }
        return ClubGiftInfoOutCodec.decodeBody(body)
    }

    fun clubOffers(
        catalog: Catalog,
        source: Int,
        expireTimestamp: Int,
    ): ClubOffersOut {
        val offers = catalog.membershipOffers.forSource(source)
        return ClubOffersOutCodec.decodeBody(
            bytesOf { m ->
                m.int(offers.size)
                offers.forEach { it.write(m, expireTimestamp) }
                m.int(source)
            },
        )
    }

    fun targetedOffer(
        offer: TargetedOffer,
        purchase: PlayerOfferPurchase,
    ): TargetedOfferOut = offer.message(purchase)

    fun bundleDiscount(rules: BundleDiscount): BundleDiscountRulesetOut =
        BundleDiscountRulesetOut(
            rules.maximumItems,
            rules.batchSize,
            rules.freeItemsPerBatch,
            rules.minimumDiscountsForBonus,
            rules.additionalThresholds,
        )

    fun giftWrapping(
        price: Int,
        wrapperIds: Collection<Int>,
        boxTypes: Collection<Int>,
        ribbonTypes: Collection<Int>,
        furnitureIds: Collection<Int>,
    ): GiftWrappingConfigurationOut =
        GiftWrappingConfigurationOut(
            true,
            price,
            wrapperIds.toList(),
            boxTypes.toList(),
            ribbonTypes.toList(),
            furnitureIds.toList(),
        )

    fun buildPassStatus(
        secondsLeft: Int,
        furniLimit: Int,
        maxFurniLimit: Int,
        secondsLeftWithGrace: Int,
    ): BuildPassSubscriptionStatusOut =
        BuildPassSubscriptionStatusOut(
            maxOf(0, secondsLeft),
            maxOf(0, furniLimit),
            maxOf(0, maxFurniLimit),
            maxOf(0, secondsLeftWithGrace),
        )

    fun buildPassFurniCount(count: Int) = BuildPassFurniCountOut(count)

    fun placementWarning(
        kind: PlacementKind,
        pageId: Int,
        offerId: Int,
        extraParam: String?,
        x: Int,
        y: Int,
        direction: Int,
        wallLocation: String?,
    ): BuildPassPlacementWarningOut {
        val w = PacketWriter()
        w
            .int(kind.code)
            .int(pageId)
            .int(offerId)
            .string(extraParam.orEmpty())
        if (kind == PlacementKind.FLOOR) {
            w.int(x).int(y).int(direction)
        } else {
            w.string(wallLocation.orEmpty())
        }
        return BuildPassPlacementWarningOut(w.toByteArray())
    }

    fun petBreeds(
        petName: String,
        races: List<PetPalette>,
    ): SellablePetPalettesOut =
        SellablePetPalettesOut(
            petName,
            races
                .sortedWith(compareBy<PetPalette> { it.breedId }.thenBy { it.paletteId })
                .map {
                    SellablePetPalettesOut.List1Item(
                        it.petType,
                        it.breedId,
                        it.paletteId,
                        it.sellable,
                        it.rare,
                    )
                },
        )

    fun petReceived(
        pet: Pet,
        gift: Boolean,
    ): PetReceivedOut {
        val from = PacketReader(bytesOf { pet.writeTo(it) })
        val id = from.int()
        val name = from.string()
        val type = from.int()
        val palette = from.int()
        val color = from.string()
        val breed = from.int()
        val parts =
            List(
                from.countInt(PET_PART_BYTES),
            ) { PetReceivedOut.List1Item(from.int(), from.int(), from.int()) }
        val level = from.int()
        return PetReceivedOut(
            gift,
            id,
            name,
            type,
            palette,
            color,
            breed,
            parts,
            level,
            (pet as? MonsterPlant)?.rarity ?: -1,
        )
    }

    fun recyclerPrizes(
        entries: List<RecyclerPrizes.Entry>,
        odds: IntUnaryOperator,
    ): RecyclerPrizesOut {
        val levels = RecyclerPrizes.byLevel(entries)
        val w = PacketWriter().int(levels.size)
        levels.forEach { (level, prizes) ->
            w.int(level).int(odds.applyAsInt(level)).int(prizes.size)
            prizes.forEach {
                w
                    .string(it.productCode)
                    .int(1)
                    .string(it.productType)
                    .int(it.typeId)
            }
        }
        return RecyclerPrizesOut(w.toByteArray())
    }

    fun marketplaceOffers(
        page: ListingPage,
        fee: MarketplaceFee = MarketplaceFee.Standard,
    ): MarketPlaceOffersOut {
        val offers = page.listings
        val w = PacketWriter().int(offers.size)
        offers.forEach { offer ->
            w
                .int(offer.offerId)
                .int(1)
                .int(offer.type)
                .int(offer.spriteId)
            listing(w, offer)
            w.int(fee.buyerPays(offer.price)).int(0)
            w.int(fee.buyerPays(offer.averagePrice)).int(offer.listingCount)
        }
        w.int(page.kindsFound)
        return MarketPlaceOffersOut(w.toByteArray())
    }

    fun marketplaceOwnOffers(
        offers: List<MarketplaceListing>,
        uncollectedCredits: Int,
        now: Int,
        fee: MarketplaceFee = MarketplaceFee.Standard,
    ): MarketPlaceOwnOffersOut {
        val w = PacketWriter().int(offers.size)
        for (offer in offers) {
            w
                .int(offer.offerId)
                .int(offer.state.code)
                .int(offer.type)
                .int(offer.spriteId)
            listing(w, offer)
            w.int(offer.price)
            val minutesLeft = (offer.listedAt + MarketplacePrices.OPEN_SECONDS - now) / SECONDS_PER_MINUTE
            w.int(if (offer.state == ListingState.OPEN) minutesLeft else 0)
            w.int(if (fee is MarketplaceFee.SellerPays) fee.sellerGets(offer.price) else 0)
            when (offer.state) {
                ListingState.SOLD -> w.long(offer.soldAt * MILLIS_PER_SECOND)
                ListingState.CLOSED -> w.long((offer.listedAt + MarketplacePrices.OPEN_SECONDS) * MILLIS_PER_SECOND)
                else -> Unit
            }
        }
        return MarketPlaceOwnOffersOut(uncollectedCredits, w.toByteArray())
    }

    private fun listing(
        w: PacketWriter,
        offer: MarketplaceListing,
    ) {
        when (offer.type) {
            MARKETPLACE_LIMITED -> w.int(offer.editionNumber).int(offer.editionSize)
            MARKETPLACE_WALL -> w.string("")
            else -> w.int(0).string("")
        }
    }

    fun marketplaceItemStats(
        marketplace: Marketplace,
        category: Int,
        itemId: Int,
    ): MarketplaceItemStatsOut =
        MarketplaceItemStatsOutCodec.decodeBody(
            bytesOf {
                marketplace.stats.writeItemStats(category, itemId, it)
            },
        )

    fun marketplaceConfiguration(settings: CatalogSettings) =
        MarketplaceConfigurationOut(
            boolean1 = true,
            int1 = MarketplacePrices.COMMISSION_PERCENT,
            int2 = settings.tokenBatchPrice,
            int3 = settings.tokenBatchSize,
            int4 = MarketplacePrices.MINIMUM_LISTING,
            int5 = MarketplacePrices.MAXIMUM_LISTING,
            int6 = MarketplacePrices.OPEN_HOURS,
            int7 = MarketplacePrices.AVERAGE_DAYS,
            int8 = settings.marketplaceFee.shownPercent,
            int9 = 1_000_000,
            int10 = settings.marketplaceFee.shownLimit,
        )
}
