package zel.protocol.incoming.catalog

import zel.db.Sql
import zel.db.SqlFailure
import zel.game.catalog.CatalogOffer
import zel.game.catalog.CatalogServices
import zel.game.catalog.Prices
import zel.game.items.Item
import zel.game.items.ProductKind
import zel.game.players.Player
import zel.net.clients.ClientConnection
import zel.protocol.outgoing.catalog.CatalogReplies
import zel.protocol.outgoing.catalog.PurchaseError
import zel.protocol.outgoing.catalog.PurchaseNotAllowed
import org.slf4j.Logger
import org.slf4j.LoggerFactory

internal class GiftOffers(
    private val services: CatalogServices,
) {
    class Price(
        val credits: Int,
        val points: Int,
    )

    class Box(
        val item: Item,
        val price: Int,
    )

    class Boxed(
        val item: CatalogOffer,
        val box: Box,
    ) {
        val price = Price(Prices.sum(item.price.credits, box.price), item.price.points)
    }

    fun pick(
        client: ClientConnection,
        sender: Player,
        request: GiftRequest,
    ): Boxed? {
        val item = offerOf(request, sender)
        return when {
            item == null || item.products.isEmpty() -> {
                refuse(client)
            }

            item.clubOnly && !sender.stats.club.isMember -> {
                CatalogReplies.purchaseNotAllowed(client, PurchaseNotAllowed.CLUB_ONLY)
                null
            }

            item.isLimited && services.raffles.blocks(item) -> {
                refuse(client)
            }

            !item.giftable -> {
                fail(client)
            }

            else -> {
                val box = boxFor(request) ?: return fail(client)
                Boxed(item, box).takeIf { affordable(client, sender, it) }
            }
        }
    }

    private fun affordable(
        client: ClientConnection,
        sender: Player,
        boxed: Boxed,
    ): Boolean {
        val pointsType = boxed.item.price.pointsType
        val creditsShort = boxed.price.credits > sender.profile.credits
        val pointsShort = boxed.price.points > sender.profile.balance(pointsType)
        if (creditsShort || pointsShort) CatalogReplies.notEnoughBalance(client, creditsShort, pointsShort, pointsType)
        return !creditsShort && !pointsShort
    }

    fun receiverWearsBadge(
        item: CatalogOffer,
        receiver: Player?,
        receiverId: Int,
    ): Boolean {
        val codes = item.products.filter { it.type == ProductKind.BADGE }.map { it.name }
        return when {
            codes.isEmpty() -> false
            receiver != null -> codes.any { receiver.inventory.badges.owns(it) }
            else -> codes.any { storedBadge(receiverId, it) }
        }
    }

    fun fitsInBox(
        client: ClientConnection,
        item: CatalogOffer,
    ): Boolean {
        val boxable = item.products.none(::staysOutOfBox)
        if (!boxable) fail(client)
        return boxable
    }

    private fun offerOf(
        request: GiftRequest,
        sender: Player,
    ): CatalogOffer? {
        val page = services.catalog.page(request.pageId) ?: return null
        if (!page.sellsTo(sender.profile.rank.id, sender.stats.club.isMember)) return null
        return page.offer(request.itemId)
            ?: synchronized(page.offers) { page.offers.values.firstOrNull { it.clientOfferId == request.itemId } }
    }

    private fun staysOutOfBox(product: Item): Boolean =
        product.isBot ||
            product.isPet ||
            product.type == ProductKind.AVATAR_EFFECT ||
            product.type == ProductKind.BADGE ||
            product.interaction.isGroupFurni

    private fun boxFor(request: GiftRequest): Box? {
        val settings = services.settings
        if (request.color !in settings.giftBoxTypes || request.ribbonId !in settings.giftRibbonTypes) {
            return null
        }
        val spriteId = request.spriteId
        val wrapping = services.catalog.contents.giftWrapping()
        val fancyPrice = { settings.giftWrappingPrice }
        val (definitionId, price) =
            wrapping.wrappers[spriteId]?.let { it to fancyPrice() }
                ?: wrapping.furniture[spriteId]?.let { it to 0 }
                ?: return null
        return services.items.getItem(definitionId)?.let { Box(it, price) }
    }

    private fun storedBadge(
        playerId: Int,
        code: String,
    ): Boolean =
        try {
            Sql(services.dataSource).select(OWNS_BADGE, playerId, code).first { true } ?: false
        } catch (e: SqlFailure) {
            log.error("Badge {} of player {} not looked up", code, playerId, e)
            false
        }

    private fun refuse(client: ClientConnection): Boxed? {
        CatalogReplies.purchaseNotAllowed(client, PurchaseNotAllowed.REFUSED)
        return null
    }

    private fun fail(client: ClientConnection): Boxed? {
        CatalogReplies.purchaseFailed(client, PurchaseError.GENERIC)
        return null
    }

    private companion object {
        val log: Logger = LoggerFactory.getLogger(GiftOffers::class.java)
        const val OWNS_BADGE = "SELECT 1 FROM player_badges WHERE player_id = ? AND badge_code = ?"
    }
}
