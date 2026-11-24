package zel.game.catalog

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import zel.ext.ExtensionBus
import zel.ext.ServerEvent
import zel.game.players.Player
import zel.protocol.outgoing.catalog.PurchaseError
import zel.protocol.outgoing.catalog.PurchaseNotAllowed

class CatalogPurchases(
    private val services: CatalogServices,
    private val limited: LimitedEditions,
) {
    private val delivery = PurchaseDelivery(services)
    private val dailyCap = LimitedDailyCap(services.config, services.texts)
    private val inventoryRoom = InventoryRoom(services.config, services.texts)

    /** The buyer hears [verdict] only once the gate is released. */
    private class Stop(
        val verdict: PurchaseRefusal,
    ) : RuntimeException(verdict.toString())

    private class Request(
        val offer: CatalogOffer,
        val buyer: Player,
        val wanted: Int,
        val extraData: String,
        val free: Boolean,
        val priceOverride: OfferPrice?,
    ) {
        val pointsType: Int get() = (priceOverride ?: offer.price).pointsType
    }

    private class Bill(
        val units: Int,
        val credits: Int,
        val points: Int,
    )

    @Suppress("LongParameterList") // the catalog's purchase API
    fun purchase(
        offer: CatalogOffer?,
        buyer: Player,
        amount: Int,
        extraData: String,
        free: Boolean,
        priceOverride: OfferPrice?,
    ): Boolean {
        if (offer == null || !PurchaseGate.enter(buyer.stats)) {
            GENERIC_FAILURE.tell(buyer)
            return false
        }
        val verdict =
            try {
                settle(Request(offer, buyer, amount, extraData, free, priceOverride))
            } finally {
                PurchaseGate.leave(buyer.stats)
            }
        verdict?.tell(buyer)
        if (verdict == null) ExtensionBus.event(ServerEvent.Purchased(buyer.profile.id, offer.id, amount))
        return verdict == null
    }

    /** Null when the order went out. */
    @Suppress("TooGenericExceptionCaught") // whatever breaks mid-purchase is answered as a failure
    private fun settle(request: Request): PurchaseRefusal? =
        try {
            val units = sizeOrder(request)
            val edition = editionStock(request)
            val bill = bill(request, units)
            inventoryRoom
                .lackFor(request.buyer, InventoryRoom.loadOf(request.offer, bill.units))
                ?.let { stop(PurchaseRefusal.Full(it)) }
            val order =
                PurchaseOrder(
                    request.buyer,
                    request.offer,
                    bill.units,
                    request.extraData,
                    bill.credits,
                    bill.points,
                    request.pointsType,
                )
            if (handOut(order, edition?.let(::reserveSerial))) null else PurchaseRefusal.Silent
        } catch (stop: Stop) {
            stop.verdict
        } catch (e: Exception) {
            log.error("Purchase of offer {} by player {} broke off", request.offer.id, request.buyer.profile.id, e)
            GENERIC_FAILURE
        }

    private fun stop(verdict: PurchaseRefusal): Nothing = throw Stop(verdict)

    private fun sizeOrder(request: Request): Int {
        val units = CatalogPurchaseRules.orderedUnits(request.offer, request.wanted)
        if (request.offer.clubOnly && !request.buyer.stats.club.isMember) {
            stop(PurchaseRefusal.NotAllowed(PurchaseNotAllowed.CLUB_ONLY))
        }
        if (!CatalogPurchaseRules.badgeDisplayBadgeOwned(request.offer, request.buyer, request.extraData)) {
            stop(GENERIC_FAILURE)
        }
        return units ?: stop(NOT_ALLOWED)
    }

    private fun editionStock(request: Request): LimitedEditionStock? {
        val offer = request.offer
        if (!offer.isLimited) return null
        val stock = limited.get(offer)
        if (stock == null) {
            log.warn("Limited offer {} has no edition stock", offer.id)
            stop(GENERIC_FAILURE)
        }
        if (stock.available() == 0) stop(PurchaseRefusal.SoldOut)
        dailyCap.reached(request.buyer, offer)?.let { stop(PurchaseRefusal.Alert(it)) }
        return stock
    }

    private fun bill(
        request: Request,
        units: Int,
    ): Bill {
        val offer = request.offer
        val override = request.priceOverride
        val bill =
            when {
                request.free -> {
                    Bill(units, 0, 0)
                }

                override != null -> {
                    Bill(units, Prices.total(override.credits, units), Prices.total(override.points, units))
                }

                else -> {
                    val bulk = CatalogPurchaseRules.hasBulkDiscount(offer)
                    Bill(
                        units = if (bulk) units + services.settings.bundleDiscount.bonus(units) else units,
                        credits = Prices.total(offer.price.credits, units),
                        points = Prices.total(offer.price.points, units),
                    )
                }
            }
        val profile = request.buyer.profile
        val kind = request.pointsType
        BalanceShortfall
            .of(bill.credits, bill.points, kind, profile.credits, profile.balance(kind))
            ?.let { stop(PurchaseRefusal.Unaffordable(it)) }
        return bill
    }

    private fun reserveSerial(stock: LimitedEditionStock): LimitedReservation {
        val number = stock.reserveNumber() ?: stop(PurchaseRefusal.SoldOut)
        return LimitedReservation(stock, stock.editionSize, number)
    }

    /** The delivery itself tells the buyer what it refuses. */
    @Suppress("TooGenericExceptionCaught") // whatever stops the delivery frees the serial number first
    private fun handOut(
        order: PurchaseOrder,
        serial: LimitedReservation?,
    ): Boolean {
        val sent =
            try {
                if (order.item.products.isEmpty()) {
                    val style =
                        order.player.services.chatStyles
                            .styleOfOffer(order.item.id) ?: stop(GENERIC_FAILURE)
                    delivery.deliver(order, null, style) != null
                } else {
                    delivery.deliver(order, serial) != null
                }
            } catch (e: Exception) {
                serial?.giveBack()
                throw e
            }
        if (!sent) serial?.giveBack()
        return sent
    }

    private fun LimitedReservation.giveBack() = stock.releaseNumber(number)

    private companion object {
        val log: Logger = LoggerFactory.getLogger(CatalogPurchases::class.java)
        val GENERIC_FAILURE = PurchaseRefusal.Failed(PurchaseError.GENERIC)
        val NOT_ALLOWED = PurchaseRefusal.NotAllowed(PurchaseNotAllowed.REFUSED)
    }
}
