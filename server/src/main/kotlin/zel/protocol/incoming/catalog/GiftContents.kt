package zel.protocol.incoming.catalog

import zel.game.catalog.CatalogOffer
import zel.game.catalog.CatalogServices
import zel.game.items.GiftWrap
import zel.game.items.Item
import zel.game.items.OwnedFurni
import zel.game.players.Player
import zel.net.clients.ClientConnection
import zel.protocol.outgoing.catalog.CatalogReplies
import zel.protocol.outgoing.catalog.PurchaseError

/** The box's state holds contents, wrapping, message and sender. */
internal class GiftContents(
    private val services: CatalogServices,
) {
    /** Every furni made so far goes into [madeSoFar] for a roll-back. */
    @Suppress("LongParameterList") // one gift in flight
    fun fill(
        client: ClientConnection,
        sender: Player,
        item: CatalogOffer,
        request: GiftRequest,
        reservation: GiftLimited.Reservation?,
        madeSoFar: MutableSet<OwnedFurni>,
    ): List<OwnedFurni>? {
        val inside = ArrayList<OwnedFurni>()
        val complete =
            item.products.all { product ->
                (1..item.quantityOf(product.id)).all { addProduct(sender, item, product, request, reservation, inside) }
            }
        madeSoFar += inside
        if (!complete) CatalogReplies.purchaseFailed(client, PurchaseError.GENERIC)
        return inside.takeIf { complete }
    }

    fun wrap(
        client: ClientConnection,
        sender: Player,
        request: GiftRequest,
        receiverId: Int,
        box: Item,
        inside: List<OwnedFurni>,
    ): OwnedFurni? {
        val profile = sender.profile
        val wrap =
            GiftWrap(
                contentIds = inside.map { it.id },
                paper = request.color,
                ribbon = request.ribbonId,
                signed = request.showName,
                note = request.message,
                senderName = profile.name,
                senderLook = profile.figure,
            )
        val gift = services.items.giveFurni(receiverId, box, wrap.state(), 0, 0)
        if (gift == null) CatalogReplies.purchaseFailed(client, PurchaseError.GENERIC)
        return gift
    }

    /** A teleport comes as a linked pair; false when a furni could not be made. */
    @Suppress("LongParameterList") // see fill
    private fun addProduct(
        sender: Player,
        item: CatalogOffer,
        product: Item,
        request: GiftRequest,
        reservation: GiftLimited.Reservation?,
        inside: MutableList<OwnedFurni>,
    ): Boolean {
        if (item.name.startsWith(RENTED_BOT_PREFIX)) return false
        val number = reservation?.number ?: 0
        val stack = reservation?.stack ?: 0
        val state = services.catalog.engravings.purchasedState(sender, item, product, request.extraData) ?: return false
        val furni = services.items.newGiftContent(product, stack, number, state) ?: return false
        inside += furni
        if (!product.interaction.isTeleport) return true
        val twin = services.items.newGiftContent(product, stack, number, state) ?: return false
        inside += twin
        services.items.pairTeleports(furni.id, twin.id)
        return true
    }

    private companion object {
        const val RENTED_BOT_PREFIX = "rentable_bot_"
    }
}
