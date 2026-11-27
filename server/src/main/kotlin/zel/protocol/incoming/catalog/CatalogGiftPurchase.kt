package zel.protocol.incoming.catalog

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import zel.game.achievements.GameEvents
import zel.game.achievements.count
import zel.game.achievements.countFor
import zel.game.catalog.BuildPassBuyPages
import zel.game.catalog.CatalogOffer
import zel.game.catalog.CatalogPage
import zel.game.catalog.CatalogServices
import zel.game.catalog.CatalogWallet
import zel.game.catalog.LimitedEditionAchievements
import zel.game.catalog.PurchaseGate
import zel.game.catalog.layouts.PageForm
import zel.game.economy.EconomyLedger
import zel.game.items.OwnedFurni
import zel.game.players.Player
import zel.game.players.UserIgnores
import zel.net.clients.ClientConnection
import zel.protocol.generated.InfoGameClosingOut
import zel.protocol.generated.InfoGameClosingOutCodec
import zel.protocol.generated.PurchaseFromCatalogAsGiftIn
import zel.protocol.outgoing.catalog.CatalogMessages
import zel.protocol.outgoing.catalog.CatalogReplies
import zel.protocol.outgoing.catalog.PurchaseError
import zel.protocol.outgoing.catalog.PurchaseNotAllowed
import zel.protocol.outgoing.inventory.InventoryUpdateItem
import zel.util.SECONDS_PER_MINUTE

class CatalogGiftPurchase(
    private val services: CatalogServices,
) {
    private val receivers = GiftReceivers(services)
    private val membershipGifts = GiftClubOffer(services)
    private val offers = GiftOffers(services)
    private val editions = GiftLimited(services)
    private val contents = GiftContents(services)

    private class GiftCharge {
        var credits = 0
        var points = 0
        var pointsType = EconomyLedger.POINTS
        val furni = LinkedHashSet<OwnedFurni>()
        var edition: GiftLimited.Reservation? = null
        var settled = false

        fun recordCharge(
            price: GiftOffers.Price,
            type: Int,
        ) {
            credits = price.credits
            points = price.points
            pointsType = type
        }

        fun wasCharged(): Boolean = credits > 0 || points > 0
    }

    private class Gift(
        val client: ClientConnection,
        val sender: Player,
        val request: GiftRequest,
        val receiverId: Int,
        val charge: GiftCharge,
    ) {
        /** A gift to oneself counts for no achievement: one account would level giving and receiving alone. */
        val toSomeoneElse: Boolean get() = sender.profile.id != receiverId
    }

    fun handle(
        client: ClientConnection,
        m: PurchaseFromCatalogAsGiftIn,
    ) {
        // the player is read once: a connection that closes mid-gift clears it, and the refund belongs to the payer
        val sender = client.player ?: return
        if (!admitted(client, sender)) return fail(client)
        val charge = GiftCharge()
        try {
            buy(client, sender, m, charge)
        } catch (
            @Suppress("TooGenericExceptionCaught") e: Exception, // any failure refunds the payer in finally
        ) {
            log.error("Gift purchase by player {} broke off", sender.profile.id, e)
            fail(client)
        } finally {
            try {
                if (!charge.settled) undo(sender, charge)
            } finally {
                PurchaseGate.leave(sender.stats)
            }
        }
    }

    /**
     * A closing game says so to every sender, also to one who is sending too fast.
     * A gift is an order like any other: it waits out the gap since the player's last order and starts the next.
     */
    private fun admitted(
        client: ClientConnection,
        sender: Player,
    ): Boolean {
        val now = services.now()
        val closingAt = services.shutdownTimestamp()
        if (closingAt > 0) {
            client.send(InfoGameClosingOutCodec, InfoGameClosingOut((closingAt - now) / SECONDS_PER_MINUTE))
            return false
        }
        val stats = sender.stats
        if (now - stats.lastPurchaseTimestamp < services.settings.purchaseCooldown) return false
        if (!PurchaseGate.enter(stats)) return false
        stats.lastPurchaseTimestamp = now.toLong()
        return true
    }

    private fun buy(
        client: ClientConnection,
        sender: Player,
        m: PurchaseFromCatalogAsGiftIn,
        charge: GiftCharge,
    ) {
        val messageMax = services.config.getInt("catalog.gifts.note.max.characters", NOTE_FIELD_CHARACTERS)
        val request = GiftRequest(m, messageMax) { services.wordFilter.filter(it, sender) }
        val receiverId = receivers.idOf(request.username)
        if (receiverId == 0) return CatalogReplies.purchaseFailed(client, PurchaseError.NO_SUCH_RECEIVER)
        if (UserIgnores.ignores(services.players, services.database, receiverId, sender.profile.id)) return fail(client)
        val page = services.catalog.page(request.pageId)
        if (page != null && sellsMembership(page)) {
            membershipGifts.gift(client, sender, page, request, receiverId)
            return
        }
        if (!receivers.hasRoomForGift(receiverId)) {
            sender.alert(services.texts.text(RECEIVER_FULL))
            return fail(client)
        }
        val boxed = offers.pick(client, sender, request) ?: return
        val gift = Gift(client, sender, request, receiverId, charge)
        val receiver = services.players.find(receiverId)
        if (pay(gift, boxed, receiver)) wrapAndSend(gift, boxed, receiver)
    }

    private fun pay(
        gift: Gift,
        boxed: GiftOffers.Boxed,
        receiver: Player?,
    ): Boolean {
        val item = boxed.item
        val client = gift.client
        if (offers.receiverWearsBadge(item, receiver, gift.receiverId)) {
            CatalogReplies.purchaseFailed(client, PurchaseError.BADGE_OWNED)
            return false
        }
        if (!offers.fitsInBox(client, item)) return false
        if (item.isLimited) {
            gift.charge.edition = editions.reserve(client, gift.sender, item) ?: return false
        }
        val pointsType = item.price.pointsType
        if (!CatalogWallet.charge(gift.sender, boxed.price.credits, pointsType, boxed.price.points)) {
            CatalogReplies.purchaseNotAllowed(client, PurchaseNotAllowed.REFUSED)
            return false
        }
        gift.charge.recordCharge(boxed.price, pointsType)
        return true
    }

    private fun wrapAndSend(
        gift: Gift,
        boxed: GiftOffers.Boxed,
        receiver: Player?,
    ) {
        val charge = gift.charge
        val inside =
            contents.fill(gift.client, gift.sender, boxed.item, gift.request, charge.edition, charge.furni) ?: return
        val box =
            contents.wrap(
                gift.client,
                gift.sender,
                gift.request,
                gift.receiverId,
                boxed.box.item,
                inside,
                charge.edition,
            ) ?: return
        charge.furni += box
        receiver?.client?.send(InventoryUpdateItem(listOf(box)))
        recordSale(gift, boxed.item)
    }

    private fun recordSale(
        gift: Gift,
        item: CatalogOffer,
    ) {
        val sender = gift.sender
        val achievements = services.achievements
        if (gift.toSomeoneElse) achievements.countFor(gift.receiverId, GameEvents.CATALOG_GIFT_RECEIVED)
        if (item.isLimited) {
            sender.stats.limitedPurchases.add(item.id, services.now())
            LimitedEditionAchievements.purchased(sender, item)
        }
        gift.charge.settled = true
        CatalogReplies.purchaseOk(gift.client, CatalogMessages.purchaseOk(item))
        if (gift.toSomeoneElse) achievements.count(sender, GameEvents.CATALOG_GIFT_SENT)
    }

    private fun undo(
        sender: Player,
        charge: GiftCharge,
    ) {
        val items = requireNotNull(services.game.items)
        charge.furni.forEach { items.deleteFurni(it.dbId) }
        // the edition number goes back only after its furni are gone
        charge.edition?.let { it.stock.releaseNumber(it.number) }
        if (charge.wasCharged()) CatalogWallet.refund(sender, charge.credits, charge.pointsType, charge.points)
    }

    private fun sellsMembership(page: CatalogPage): Boolean =
        page.form is PageForm.ClubBuy || BuildPassBuyPages.isBuyPage(page)

    private fun fail(client: ClientConnection) = CatalogReplies.purchaseFailed(client, PurchaseError.GENERIC)

    private companion object {
        val log: Logger = LoggerFactory.getLogger(CatalogGiftPurchase::class.java)
        const val NOTE_FIELD_CHARACTERS = 140
        const val RECEIVER_FULL = "catalog.gifts.receiver_full"
    }
}
