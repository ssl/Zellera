package zel.protocol.incoming.catalog

import org.apache.commons.lang3.StringUtils
import org.slf4j.LoggerFactory
import zel.game.catalog.BalanceShortfall
import zel.game.catalog.BuildPassMembership
import zel.game.catalog.Catalog
import zel.game.catalog.CatalogMode
import zel.game.catalog.CatalogServices
import zel.game.catalog.CatalogWallet
import zel.game.catalog.ClubGiftClaims
import zel.game.catalog.MembershipOffer
import zel.game.catalog.NewAdditions
import zel.game.catalog.Prices
import zel.game.catalog.TargetedOffer
import zel.game.catalog.layouts.PageForm
import zel.game.items.Item
import zel.game.pets.PetNameLimits
import zel.game.players.subscriptions.ClubSettings
import zel.game.players.subscriptions.Subscription
import zel.net.clients.ClientConnection
import zel.protocol.generated.ApproveNameInCodec
import zel.protocol.generated.BuildPassQueryFurniCountInCodec
import zel.protocol.generated.BundleDiscountRulesetOutCodec
import zel.protocol.generated.CatalogIndexOutCodec
import zel.protocol.generated.CatalogPageOutCodec
import zel.protocol.generated.CatalogPageWithEarliestExpiryOutCodec
import zel.protocol.generated.ClubExtendOfferOut
import zel.protocol.generated.ClubExtendOfferOutCodec
import zel.protocol.generated.ClubGiftInfoOutCodec
import zel.protocol.generated.ClubOffersOutCodec
import zel.protocol.generated.FurniListInvalidateOut
import zel.protocol.generated.FurniListInvalidateOutCodec
import zel.protocol.generated.GetBundleDiscountRulesetInCodec
import zel.protocol.generated.GetCatalogIndexInCodec
import zel.protocol.generated.GetCatalogPageInCodec
import zel.protocol.generated.GetCatalogPageWithEarliestExpiryInCodec
import zel.protocol.generated.GetClubExtendOfferInCodec
import zel.protocol.generated.GetClubGiftInCodec
import zel.protocol.generated.GetClubOffersInCodec
import zel.protocol.generated.GetGiftWrappingConfigurationInCodec
import zel.protocol.generated.GetIsOfferGiftableInCodec
import zel.protocol.generated.GetNextTargetedOfferInCodec
import zel.protocol.generated.GetOfficialSongIdInCodec
import zel.protocol.generated.GetProductOfferInCodec
import zel.protocol.generated.GetSeasonalCalendarDailyInCodec
import zel.protocol.generated.GetSellablePetPalettesInCodec
import zel.protocol.generated.GetSongInfoInCodec
import zel.protocol.generated.GetTargetedOfferInCodec
import zel.protocol.generated.GiftWrappingConfigurationOutCodec
import zel.protocol.generated.InfoGameClosingOut
import zel.protocol.generated.InfoGameClosingOutCodec
import zel.protocol.generated.MarkCatalogNewAdditionsPageOpenedInCodec
import zel.protocol.generated.ProductOfferOutCodec
import zel.protocol.generated.PurchaseFromCatalogAsGiftInCodec
import zel.protocol.generated.PurchaseFromCatalogInCodec
import zel.protocol.generated.PurchaseTargetedOfferInCodec
import zel.protocol.generated.PurchaseVipMembershipExtensionInCodec
import zel.protocol.generated.RedeemVoucherInCodec
import zel.protocol.generated.SeasonalCalendarDailyOfferOutCodec
import zel.protocol.generated.SelectClubGiftInCodec
import zel.protocol.generated.SellablePetPalettesOutCodec
import zel.protocol.generated.SetTargetedOfferStateInCodec
import zel.protocol.generated.ShopTargetedOfferViewedInCodec
import zel.protocol.generated.TargetedOfferNotFoundOut
import zel.protocol.generated.TargetedOfferNotFoundOutCodec
import zel.protocol.generated.TargetedOfferOutCodec
import zel.protocol.outgoing.catalog.CatalogMessages
import zel.protocol.outgoing.catalog.CatalogReplies
import zel.protocol.outgoing.catalog.PetNameStatus
import zel.protocol.outgoing.catalog.PurchaseError
import zel.protocol.outgoing.rooms.items.jukebox.JukeBoxTrackCode
import zel.protocol.outgoing.rooms.items.jukebox.JukeBoxTrackData
import zel.protocol.outgoing.users.ClubGiftReceived
import zel.protocol.typed.ClientCooldown
import zel.protocol.typed.PacketPackage
import zel.protocol.typed.PacketRoute
import zel.protocol.typed.PacketSession
import zel.util.SECONDS_PER_DAY
import zel.util.SECONDS_PER_MINUTE

private const val PURCHASE_GROUP = "catalog.purchase"
private const val PURCHASE_GAP_MS = 250L
private const val SHORT_GAP_MS = 500L
private const val LONG_GAP_MS = 1000L
private const val MAX_PURCHASE_COUNT = 100
private const val VOUCHER_GAP_MS = 2000L
private const val MAX_TRACK_REQUESTS = 1_000
private const val MAX_GIFT_NAME = 100

class CatalogPackets(
    private val services: CatalogServices,
    gifts: CatalogGiftPurchase,
) : PacketPackage {
    private val log = LoggerFactory.getLogger(CatalogPackets::class.java)
    private val targetOffers = TargetOfferPurchase(services)
    private val catalog: Catalog get() = services.catalog
    private val purchaseCooldown = ClientCooldown(PURCHASE_GROUP, PURCHASE_GAP_MS)
    private val giftCooldown = ClientCooldown(PURCHASE_GROUP, SHORT_GAP_MS)
    private val targetedPurchaseCooldown = ClientCooldown(PURCHASE_GROUP, LONG_GAP_MS)
    private val clubGifts = ClubGiftClaims(services.dataSource)
    private val clubGiftCooldown = ClientCooldown("SelectClubGift", LONG_GAP_MS)
    private val voucherCooldown = ClientCooldown("RedeemVoucher", VOUCHER_GAP_MS)
    private val petPalettesCooldown = ClientCooldown("GetSellablePetPalettes", SHORT_GAP_MS)
    private val nextTargetedOfferCooldown = ClientCooldown("GetNextTargetedOffer", LONG_GAP_MS)
    private val targetedOfferCooldown = ClientCooldown("GetTargetedOffer", LONG_GAP_MS)

    override val name = "catalog"

    override val routes =
        listOf(
            PacketRoute(MarkCatalogNewAdditionsPageOpenedInCodec) { s, _ ->
                val stats = requireNotNull(s.client.player).stats
                synchronized(stats) { stats.tally.newAdditionsSeen = NewAdditions.newestOfferId(catalog) }
            },
            PacketRoute(GetCatalogIndexInCodec) { s, m ->
                val player = requireNotNull(s.client.player)
                CatalogMessages.catalogIndex(catalog, player, m.mODE)?.let { s.send(CatalogIndexOutCodec, it) }
                if (!m.mODE.equals(
                        "normal",
                        ignoreCase = true,
                    )
                ) {
                    BuildPassMembership.tellStatus(player, services.database)
                }
            },
            PacketRoute(GetCatalogPageInCodec) { s, m -> openPage(s.client, m.catalogPageId, m.offerId, m.mode) },
            PacketRoute(PurchaseFromCatalogInCodec) { s, m ->
                if (purchaseCooldown.acquire(s.client)) purchase(s.client, m.int1, m.int2, m.string1, m.int3)
            },
            PacketRoute(PurchaseFromCatalogAsGiftInCodec) { s, m ->
                if (giftCooldown.acquire(s.client)) gifts.handle(s.client, m)
            },
            PacketRoute(GetIsOfferGiftableInCodec) { s, m ->
                CatalogReplies.isOfferGiftable(s.client, m.offerId, giftable(m.offerId))
            },
            PacketRoute(RedeemVoucherInCodec) { s, m ->
                if (voucherCooldown.acquire(s.client)) redeemVoucher(s.client, m.voucherCode)
            },
            PacketRoute(GetProductOfferInCodec) { s, m ->
                val item = catalog.offerForSearchId(m.offerId, requireNotNull(s.client.player))
                if (item != null) s.send(ProductOfferOutCodec, CatalogMessages.productOffer(item))
            },
            PacketRoute(GetBundleDiscountRulesetInCodec) { s, _ ->
                s.send(
                    BundleDiscountRulesetOutCodec,
                    CatalogMessages.bundleDiscount(services.settings.bundleDiscount),
                )
            },
            PacketRoute(GetGiftWrappingConfigurationInCodec) { s, _ -> sendGiftWrapping(s) },
            PacketRoute(GetSellablePetPalettesInCodec) { s, m ->
                if (petPalettesCooldown.acquire(s.client)) {
                    services.pets.catalog.palettesOf(m.petName)?.let {
                        s.send(SellablePetPalettesOutCodec, CatalogMessages.petBreeds(m.petName, it))
                    }
                }
            },
            PacketRoute(ApproveNameInCodec) { s, m -> checkPetName(s.client, m.petName) },
            PacketRoute(GetClubOffersInCodec) { s, m ->
                val player = requireNotNull(s.client.player)
                val gift = m.int1 == MembershipOffer.SOURCE_VIP_GIFT
                val expire = if (gift) services.now() else player.stats.club.clubExpiresAt
                s.send(ClubOffersOutCodec, CatalogMessages.clubOffers(catalog, m.int1, expire))
            },
            PacketRoute(GetClubGiftInCodec) { s, _ ->
                val stats = requireNotNull(s.client.player).stats
                s.send(
                    ClubGiftInfoOutCodec,
                    CatalogMessages.clubGifts(
                        catalog,
                        stats.club.daysTillNextGift,
                        stats.club.remainingGifts,
                        stats.club.pastClubDays,
                    ),
                )
            },
            PacketRoute(SelectClubGiftInCodec) { s, m ->
                if (clubGiftCooldown.acquire(s.client)) selectClubGift(s.client, m.itemName)
            },
            PacketRoute(GetClubExtendOfferInCodec) { s, _ -> requestClubDiscount(s) },
            PacketRoute(PurchaseVipMembershipExtensionInCodec) { s, m -> buyClubDiscount(s.client, m.offerId) },
            PacketRoute(GetNextTargetedOfferInCodec) { s, _ ->
                if (nextTargetedOfferCooldown.acquire(s.client)) {
                    answerTargetOffer(s, catalog.runningTargetedOffer())
                }
            },
            PacketRoute(GetTargetedOfferInCodec) { s, m ->
                if (targetedOfferCooldown.acquire(s.client)) {
                    answerTargetOffer(s, catalog.runningTargetedOffer()?.takeIf { it.id == m.int1 })
                }
            },
            PacketRoute(ShopTargetedOfferViewedInCodec) { s, m ->
                if (catalog.targetedOffer(m.int1) != null) {
                    requireNotNull(s.client.player).stats.offerPurchaseOrNull(m.int1)?.state = m.int2
                }
            },
            PacketRoute(PurchaseTargetedOfferInCodec) { s, m ->
                if (targetedPurchaseCooldown.acquire(s.client)) targetOffers.purchase(s.client, m.offerId, m.amount)
            },
            PacketRoute(SetTargetedOfferStateInCodec) { s, m ->
                requireNotNull(s.client.player).stats.offerPurchaseOrNull(m.id)?.state = m.state
            },
            PacketRoute(GetCatalogPageWithEarliestExpiryInCodec) { s, _ ->
                val player = requireNotNull(s.client.player)
                val page = catalog.pageEndingFirst(player.profile.rank.id, player.stats.club.isMember)
                s.send(CatalogPageWithEarliestExpiryOutCodec, CatalogMessages.pageEndingFirst(page))
            },
            PacketRoute(GetSeasonalCalendarDailyInCodec) { s, _ ->
                val player = requireNotNull(s.client.player)
                catalog.calendarDailyOffer(player.profile.rank.id, player.stats.club.isMember)?.let {
                    s.send(SeasonalCalendarDailyOfferOutCodec, CatalogMessages.calendarDailyOffer(it))
                }
            },
            PacketRoute(BuildPassQueryFurniCountInCodec) { s, _ ->
                BuildPassMembership.tellStatus(requireNotNull(s.client.player), services.database)
            },
            PacketRoute(GetOfficialSongIdInCodec) { s, m ->
                services.items
                    .jukeboxTrack(m.songName)
                    ?.let { s.client.send(JukeBoxTrackCode(it)) }
            },
            PacketRoute(GetSongInfoInCodec) { s, m ->
                if (m.songIds.size <= MAX_TRACK_REQUESTS) {
                    val tracks = m.songIds.mapNotNull { services.items.jukeboxTrack(it) }
                    s.client.send(JukeBoxTrackData(tracks))
                }
            },
        )

    private fun openPage(
        client: ClientConnection,
        pageId: Int,
        offerId: Int,
        mode: String,
    ) {
        val page = catalog.page(pageId, CatalogMode.fromCode(mode))
        if (pageId <= 0 || page == null) return
        val player = requireNotNull(client.player)
        if (page.opensFor(player.profile.rank.id)) {
            CatalogReplies.send(
                client,
                CatalogPageOutCodec,
                CatalogMessages.catalogPage(catalog, page, offerId, mode),
            )
        } else if (!page.info.enabled) {
            client.warnings.refused("disabled catalog page")
        }
    }

    private fun purchase(
        client: ClientConnection,
        pageId: Int,
        itemId: Int,
        extraData: String,
        count: Int,
    ) {
        val stats = requireNotNull(client.player).stats
        if (services.now() - stats.lastPurchaseTimestamp < services.settings.purchaseCooldown) {
            CatalogReplies.purchaseFailed(client, PurchaseError.GENERIC)
            return
        }
        stats.lastPurchaseTimestamp = services.now().toLong()
        if (services.shutdownTimestamp() > 0) {
            sendClosing(client)
            return
        }
        ShopCheckout(client, services)
            .place(ShopOrder(pageId, itemId, extraData, count.coerceIn(1, MAX_PURCHASE_COUNT)))
    }

    private fun sendClosing(client: ClientConnection) =
        client.send(
            InfoGameClosingOutCodec,
            InfoGameClosingOut((services.shutdownTimestamp() - services.now()) / SECONDS_PER_MINUTE),
        )

    private fun giftable(offerId: Int): Boolean {
        catalog.offer(offerId)?.let { return it.giftable }
        return catalog.contents.memberships[offerId]?.giftable == true
    }

    private fun redeemVoucher(
        client: ClientConnection,
        code: String,
    ) {
        if (services.shutdownTimestamp() > 0) sendClosing(client) else catalog.vouchers.redeem(client, code)
    }

    private fun sendGiftWrapping(s: PacketSession) {
        val wrapping = catalog.contents.giftWrapping()
        s.send(
            GiftWrappingConfigurationOutCodec,
            CatalogMessages.giftWrapping(
                services.settings.giftWrappingPrice,
                wrapping.wrappers.keys,
                services.settings.giftBoxTypes,
                services.settings.giftRibbonTypes,
                wrapping.furniture.keys,
            ),
        )
    }

    private fun checkPetName(
        client: ClientConnection,
        petName: String,
    ) {
        val min = PetNameLimits.minimum
        val max = PetNameLimits.maximum
        when {
            petName.length < min -> CatalogReplies.petNameStatus(client, PetNameStatus.TOO_SHORT, min.toString())

            petName.length > max -> CatalogReplies.petNameStatus(client, PetNameStatus.TOO_LONG, max.toString())

            !StringUtils.isAlphanumeric(
                petName,
            ) -> CatalogReplies.petNameStatus(client, PetNameStatus.BAD_CHARACTER, petName)

            else -> CatalogReplies.petNameStatus(client, PetNameStatus.OK, petName)
        }
    }

    @Suppress("ReturnCount") // each refusal answers a purchase failure
    private fun selectClubGift(
        client: ClientConnection,
        itemName: String,
    ) {
        val player = requireNotNull(client.player)
        val club = player.stats.club
        val failed = { CatalogReplies.purchaseFailed(client, PurchaseError.GENERIC) }
        if (itemName.isEmpty() || itemName.length > MAX_GIFT_NAME || club.remainingGifts < 1) return failed()
        val gift =
            catalog
                .pageWithRole(PageForm.Role.CLUB_GIFT)
                ?.let { page -> synchronized(page.offers) { page.offers.values.toList() } }
                ?.firstOrNull { it.name.equals(itemName, ignoreCase = true) }
                ?: return failed()
        val daysRequired = gift.presetState.toIntOrNull() ?: 0
        if (daysRequired > club.pastClubDays || !clubGifts.take(player)) return failed()
        val given = gift.products.filterTo(LinkedHashSet()) { giveClubGift(player.profile.id, it) }
        if (given.isEmpty()) {
            log.warn("Club gift offer {} gave player {} nothing", gift.id, player.profile.id)
            clubGifts.giveBack(player)
            return failed()
        }
        client.send(ClubGiftReceived(gift.name, given))
        client.send(FurniListInvalidateOutCodec, FurniListInvalidateOut)
    }

    private fun giveClubGift(
        playerId: Int,
        product: Item,
    ): Boolean = services.items.giveFurni(playerId, product, "", 0, 0) != null

    private fun discountWindowDays(client: ClientConnection): Int? {
        val subscription = requireNotNull(client.player).stats.club.get(Subscription.CLUB)
        val remaining = subscription?.secondsLeft ?: 0
        var days = Math.floor(remaining / SECONDS_PER_DAY.toDouble()).toInt()
        val minutes = Math.ceil(remaining / SECONDS_PER_MINUTE.toDouble()).toInt()
        if (subscription != null && days < 1 && minutes > 0) days = 1
        return if (remaining > 0 && ClubSettings.discount.covers(days)) days else null
    }

    @Suppress("ReturnCount") // any missing piece means there is no offer to show
    private fun requestClubDiscount(session: PacketSession) {
        val days = discountWindowDays(session.client) ?: return
        val deal =
            catalog.contents.memberships.values
                .firstOrNull { it.isDeal } ?: return
        val regular = catalog.membershipOffers.listed().firstOrNull { it.days == deal.days } ?: return
        val player = session.client.player ?: return
        val after = deal.afterPurchase(player.stats.club.clubExpiresAt)
        session.send(
            ClubExtendOfferOutCodec,
            ClubExtendOfferOut(
                deal.id,
                deal.name.orEmpty(),
                false,
                deal.price.credits,
                deal.price.points,
                deal.price.pointsType,
                deal.kind == MembershipOffer.Kind.VIP,
                after.months,
                deal.days,
                deal.giftable,
                after.daysAhead,
                after.lastDay.year,
                after.lastDay.monthValue,
                after.lastDay.dayOfMonth,
                regular.price.credits,
                regular.price.points,
                regular.price.pointsType,
                maxOf(0, days),
            ),
        )
    }

    @Suppress("ReturnCount") // each failed check drops the purchase
    private fun buyClubDiscount(
        client: ClientConnection,
        offerId: Int,
    ) {
        discountWindowDays(client) ?: return
        val deal = catalog.contents.memberships[offerId]?.takeIf { it.isDeal } ?: return
        if (catalog.membershipOffers.listed().none { it.days == deal.days }) return
        val player = requireNotNull(client.player)
        val seconds =
            try {
                Prices.membershipSeconds(deal.price, deal.days)
            } catch (e: IllegalArgumentException) {
                log.debug("discount club offer {} has an invalid price: {}", offerId, e.message)
                CatalogReplies.purchaseFailed(client, PurchaseError.GENERIC)
                return
            }
        if (seconds <= 0) return
        val profile = player.profile
        val shortfall =
            BalanceShortfall.of(
                deal.price.credits,
                deal.price.points,
                deal.price.pointsType,
                profile.credits,
                profile.balance(deal.price.pointsType),
            )
        if (shortfall != null) {
            shortfall.send(client)
            return
        }
        if (!CatalogWallet.charge(player, deal.price.credits, deal.price.pointsType, deal.price.points)) {
            CatalogReplies.purchaseFailed(client, PurchaseError.GENERIC)
            return
        }
        val confirm = { CatalogReplies.purchaseOk(client, CatalogMessages.purchaseOkClubOffer(deal)) }
        if (player.stats.club.create(Subscription.CLUB, seconds, confirm) == null) {
            CatalogWallet.refund(player, deal.price.credits, deal.price.pointsType, deal.price.points)
            CatalogReplies.purchaseFailed(client, PurchaseError.GENERIC)
            return
        }
        player.stats.run()
    }

    private fun answerTargetOffer(
        s: PacketSession,
        offer: TargetedOffer?,
    ) {
        val player = s.client.player ?: return
        val purchase = offer?.let { player.stats.offerPurchase(it.id) }
        if (offer == null || purchase == null) {
            s.send(TargetedOfferNotFoundOutCodec, TargetedOfferNotFoundOut)
        } else {
            s.send(TargetedOfferOutCodec, CatalogMessages.targetedOffer(offer, purchase))
        }
    }
}
