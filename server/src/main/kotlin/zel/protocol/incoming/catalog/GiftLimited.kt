package zel.protocol.incoming.catalog

import zel.game.catalog.CatalogOffer
import zel.game.catalog.CatalogServices
import zel.game.catalog.LimitedDailyCap
import zel.game.catalog.LimitedEditionStock
import zel.game.players.Player
import zel.net.clients.ClientConnection
import zel.protocol.outgoing.catalog.CatalogReplies

internal class GiftLimited(
    private val services: CatalogServices,
) {
    private val dailyCap = LimitedDailyCap(services.config, services.texts)

    class Reservation(
        val stock: LimitedEditionStock,
        val number: Int,
        val offerId: Int,
        val stack: Int,
    )

    fun reserve(
        client: ClientConnection,
        sender: Player,
        item: CatalogOffer,
    ): Reservation? {
        val editions = services.catalog.limited
        if (editions.get(item)?.available() == 0) {
            CatalogReplies.limitedSoldOut(client)
            return null
        }
        dailyCap.reached(sender, item)?.let {
            sender.alert(it)
            return null
        }
        val stock = editions.get(item) ?: editions.createOrUpdate(item) ?: return null
        val number = stock.reserveNumber()
        if (number == null) CatalogReplies.limitedSoldOut(client)
        return number?.let { Reservation(stock, it, item.id, stock.editionSize) }
    }
}
