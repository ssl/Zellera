package zel.protocol.incoming.catalog

import zel.game.catalog.BalanceShortfall
import zel.game.catalog.BuildPassBuyPages
import zel.game.catalog.BuildPassMembership
import zel.game.catalog.CatalogOffer
import zel.game.catalog.CatalogPage
import zel.game.catalog.CatalogServices
import zel.game.catalog.CatalogWallet
import zel.game.catalog.MembershipOffer
import zel.game.catalog.OfferPrice
import zel.game.catalog.PetOrders
import zel.game.catalog.Prices
import zel.game.catalog.PurchaseGate
import zel.game.catalog.RoomBundles
import zel.game.catalog.layouts.PageForm
import zel.game.permissions.PlayerLimits
import zel.game.pets.PetNameLimits
import zel.game.players.Player
import zel.game.players.subscriptions.Subscription
import zel.net.clients.ClientConnection
import zel.protocol.generated.PurchaseOKOut
import zel.protocol.navigator.CanCreateRoom
import zel.protocol.outgoing.catalog.CatalogMessages
import zel.protocol.outgoing.catalog.CatalogReplies
import zel.protocol.outgoing.catalog.PurchaseError
import zel.protocol.outgoing.catalog.PurchaseNotAllowed

/** One order from the shop window: a membership, a room bundle or the goods of an offer. */
class ShopCheckout(
    private val client: ClientConnection,
    private val services: CatalogServices,
    private val roomBundles: RoomBundles = RoomBundles(services),
) {
    private sealed interface Sale {
        data object NoPage : Sale

        data class Membership(
            val page: CatalogPage,
        ) : Sale

        data class RoomBundle(
            val page: CatalogPage,
        ) : Sale

        data class Furni(
            val page: CatalogPage,
        ) : Sale
    }

    private class MembershipBill(
        val days: Int,
        val price: OfferPrice,
        val termSeconds: Int,
        val furniBonus: Int,
    ) {
        companion object {
            /** Null when the multiplied amounts do not fit. */
            fun of(
                offer: MembershipOffer,
                count: Int,
            ): MembershipBill? =
                try {
                    val days = Prices.total(offer.days, count)
                    val forBuildPass = offer.kind == MembershipOffer.Kind.BUILD_PASS
                    MembershipBill(
                        days = days,
                        price =
                            OfferPrice(
                                Prices.total(offer.price.credits, count),
                                Prices.total(offer.price.points, count),
                                offer.price.pointsType,
                            ),
                        termSeconds = if (offer.kind.addsDays) Prices.subscriptionSeconds(days) else 0,
                        furniBonus =
                            if (forBuildPass) {
                                Prices.total(BuildPassMembership.monthFurniBonus(offer.days), count)
                            } else {
                                0
                            },
                    )
                } catch (_: IllegalArgumentException) {
                    null
                }
        }
    }

    fun place(order: ShopOrder) {
        // Read once: a closing connection clears its player; payment, lock and refund stay with the payer.
        val payer = client.player ?: return
        when (val sale = saleFor(order, payer)) {
            Sale.NoPage -> failed()
            is Sale.Membership -> oneAtATime(payer) { sellMembership(sale.page, payer, order) }
            is Sale.RoomBundle -> oneAtATime(payer) { sellRoomBundle(sale.page, payer) }
            is Sale.Furni -> sellFurni(sale.page, payer, order)
        }
    }

    private fun saleFor(
        order: ShopOrder,
        payer: Player,
    ): Sale {
        val page =
            order.purchasePage({ services.catalog.shopOfferForSearchId(it) }, { services.catalog.page(it) }) {
                it.sellsTo(payer.profile.rank.id, payer.stats.club.isMember)
            }
        return when {
            page == null -> Sale.NoPage
            page.form.role == PageForm.Role.ROOM_BUNDLE && !order.namesNoPage -> Sale.RoomBundle(page)
            page.form is PageForm.ClubBuy || BuildPassBuyPages.isBuyPage(page) -> Sale.Membership(page)
            else -> Sale.Furni(page)
        }
    }

    private fun oneAtATime(
        payer: Player,
        sale: () -> Unit,
    ) {
        if (!PurchaseGate.enter(payer.stats)) return failed()
        try {
            sale()
        } finally {
            PurchaseGate.leave(payer.stats)
        }
    }

    private fun sellMembership(
        page: CatalogPage,
        payer: Player,
        order: ShopOrder,
    ) {
        val offer = services.catalog.membershipOffers.forPage(page, order.itemId)
        if (offer == null || !offer.soldIn(BuildPassBuyPages.window(page))) return failed()
        // Upgrades are members-only; without a membership only a crafted packet gets here.
        if (!offer.kind.addsDays && !payer.stats.club.has(Subscription.BUILD_PASS)) return notAllowed()
        val bill = MembershipBill.of(offer, order.count) ?: return notAllowed()
        if (bill.days <= 0) return
        bill.price.lackOf(payer)?.let { return it.send(client) }
        if (!bill.price.takeFrom(payer)) return notAllowed()
        val receipt = receiptFor(page, offer, order.itemId)
        if (grantMembership(payer, offer, bill, receipt)) payer.stats.run()
    }

    private fun receiptFor(
        page: CatalogPage,
        offer: MembershipOffer,
        itemId: Int,
    ): PurchaseOKOut =
        (if (BuildPassBuyPages.isBuyPage(page)) page.offer(itemId) else null)
            ?.let { CatalogMessages.purchaseOkClubPage(it, offer) }
            ?: CatalogMessages.purchaseOkClubOffer(offer)

    private fun grantMembership(
        payer: Player,
        offer: MembershipOffer,
        bill: MembershipBill,
        receipt: PurchaseOKOut,
    ): Boolean {
        val confirm = { CatalogReplies.purchaseOk(client, receipt) }
        if (!offer.kind.addsDays) return grantUpgrade(payer, bill, confirm)
        val club = payer.stats.club
        val kind =
            if (offer.kind == MembershipOffer.Kind.BUILD_PASS) Subscription.BUILD_PASS else Subscription.CLUB
        val confirmBeforeStatus = kind == Subscription.CLUB
        // the month bonus is added first, so the status the extension sends already carries it
        if (!club.addBuildPassBonusFurni(bill.furniBonus)) return refund(payer, bill)
        val started = club.create(kind, bill.termSeconds) { if (confirmBeforeStatus) confirm() } != null
        if (!started) {
            club.buildPassBonusFurni -= bill.furniBonus
            return refund(payer, bill)
        }
        if (!confirmBeforeStatus) confirm()
        return true
    }

    private fun grantUpgrade(
        payer: Player,
        bill: MembershipBill,
        confirm: () -> Unit,
    ): Boolean {
        if (!payer.stats.club.addBuildPassBonusFurni(bill.days)) return refund(payer, bill)
        CatalogReplies.buildPassFurniCount(
            client,
            BuildPassMembership.trackedFurniCount(services.database, payer.profile.id),
        )
        CatalogReplies.buildPassStatus(client, payer)
        confirm()
        return true
    }

    private fun refund(
        payer: Player,
        bill: MembershipBill,
    ): Boolean {
        bill.price.returnTo(payer)
        failed()
        return false
    }

    private fun sellFurni(
        page: CatalogPage,
        payer: Player,
        order: ShopOrder,
    ) {
        val offer =
            page.offer(order.itemId)
                ?: synchronized(page.offers) { page.offers.values.firstOrNull { it.clientOfferId == order.itemId } }
        if (offer?.products.orEmpty().any { it.isPet } && !namesPet(order.extraData)) return failed()
        if (services.raffles.join(offer, payer)) return
        services.catalog.purchase(offer, payer, order.count, order.extraData, false)
    }

    private fun namesPet(extraData: String): Boolean {
        // trailing empty lines do not count in the three-line request
        val lines = extraData.split("\n").let { if (it.size == 1) it else it.dropLastWhile(String::isEmpty) }
        val petName = lines.firstOrNull().orEmpty()
        return lines.size == PetOrders.PARTS &&
            petName.length in PetNameLimits.minimum..PetNameLimits.maximum &&
            petName.codePoints().allMatch(Character::isLetterOrDigit)
    }

    private fun sellRoomBundle(
        page: CatalogPage,
        payer: Player,
    ) {
        val bundle = page.offers.values.firstOrNull() ?: return failed()
        if (!roomBundleAllowed(bundle, payer)) return
        if (!bundle.price.takeFrom(payer)) return notAllowed()
        var copied = false
        try {
            copied = roomBundles.copyFor(page, payer)
        } finally {
            if (!copied) bundle.price.returnTo(payer)
        }
        if (copied) CatalogReplies.purchaseOk(client, CatalogMessages.purchaseOk(null)) else failed()
    }

    private fun roomBundleAllowed(
        bundle: CatalogOffer,
        payer: Player,
    ): Boolean {
        if (bundle.price.credits < 0 || bundle.price.points < 0) {
            notAllowed()
            return false
        }
        bundle.price.lackOf(payer)?.let {
            it.send(client)
            return false
        }
        return withinRoomLimit(payer)
    }

    private fun withinRoomLimit(payer: Player): Boolean {
        val owned = services.game.roomListings.countOwned(payer.profile.id)
        val allowed = PlayerLimits.maxRooms(payer)
        if (owned >= allowed) client.send(CanCreateRoom(owned, allowed))
        return owned < allowed
    }

    private fun OfferPrice.lackOf(payer: Player): BalanceShortfall? =
        BalanceShortfall.of(credits, points, pointsType, payer.profile.credits, payer.profile.balance(pointsType))

    private fun OfferPrice.takeFrom(payer: Player): Boolean = CatalogWallet.charge(payer, credits, pointsType, points)

    private fun OfferPrice.returnTo(payer: Player) = CatalogWallet.refund(payer, credits, pointsType, points)

    private fun failed() = CatalogReplies.purchaseFailed(client, PurchaseError.GENERIC)

    private fun notAllowed() = CatalogReplies.purchaseNotAllowed(client, PurchaseNotAllowed.REFUSED)
}
