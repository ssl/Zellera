package zel.game.catalog

import zel.game.players.Player
import zel.protocol.outgoing.catalog.CatalogReplies
import zel.protocol.outgoing.catalog.PurchaseError
import zel.protocol.outgoing.catalog.PurchaseNotAllowed

sealed interface PurchaseRefusal {
    data class Failed(
        val error: PurchaseError,
    ) : PurchaseRefusal

    data class NotAllowed(
        val reason: PurchaseNotAllowed,
    ) : PurchaseRefusal

    data object SoldOut : PurchaseRefusal

    data class Alert(
        val text: String,
    ) : PurchaseRefusal

    /** The order does not fit the inventory: the purchase fails and [text] says which one is full. */
    data class Full(
        val text: String,
    ) : PurchaseRefusal

    data class Unaffordable(
        val shortfall: BalanceShortfall,
    ) : PurchaseRefusal

    data object Silent : PurchaseRefusal

    fun tell(player: Player) {
        val client = player.client ?: return
        when (this) {
            is Failed -> {
                CatalogReplies.purchaseFailed(client, error)
            }

            is NotAllowed -> {
                CatalogReplies.purchaseNotAllowed(client, reason)
            }

            SoldOut -> {
                CatalogReplies.limitedSoldOut(client)
            }

            is Alert -> {
                player.alert(text)
            }

            is Full -> {
                CatalogReplies.purchaseFailed(client, PurchaseError.GENERIC)
                player.alert(text)
            }

            is Unaffordable -> {
                shortfall.send(client)
            }

            Silent -> {}
        }
    }
}
