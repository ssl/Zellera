package zel.protocol.incoming.catalog

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import zel.db.allOrNothing
import zel.game.catalog.CatalogOffer
import zel.game.catalog.CatalogServices
import zel.game.items.GiftWrap
import zel.game.items.Item
import zel.game.items.OwnedFurni
import zel.game.players.Player
import zel.net.clients.ClientConnection
import zel.protocol.outgoing.catalog.CatalogReplies
import zel.protocol.outgoing.catalog.PurchaseError
import java.sql.SQLException

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

    @Suppress("LongParameterList") // one gift in flight
    fun wrap(
        client: ClientConnection,
        sender: Player,
        request: GiftRequest,
        receiverId: Int,
        box: Item,
        inside: List<OwnedFurni>,
        reservation: GiftLimited.Reservation?,
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
        val state = wrap.state()
        if (state == null) {
            CatalogReplies.purchaseFailed(client, PurchaseError.GENERIC)
            return null
        }
        val gift = writeBox(sender, receiverId, box, state, inside, reservation)
        if (gift == null) {
            CatalogReplies.purchaseFailed(client, PurchaseError.GENERIC)
            return null
        }
        services.items.deliverGift(receiverId, gift)
        return gift
    }

    private fun writeBox(
        sender: Player,
        receiverId: Int,
        box: Item,
        state: String,
        inside: List<OwnedFurni>,
        reservation: GiftLimited.Reservation?,
    ): OwnedFurni? =
        try {
            services.database.dataSource.connection.use { connection ->
                connection.allOrNothing {
                    services.items.newFurni(connection, receiverId, box, 0, 0, state)?.also {
                        inside.forEach { item -> reservation?.stock?.markSold(connection, sender, item) }
                    }
                }
            }
        } catch (e: SQLException) {
            log.error("Wrapping a gift for player {} failed", receiverId, e)
            null
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
        val log: Logger = LoggerFactory.getLogger(GiftContents::class.java)
        const val RENTED_BOT_PREFIX = "rentable_bot_"
    }
}
