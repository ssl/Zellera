package zel.game.catalog

import zel.game.players.Player

data class PurchaseOrder(
    val player: Player,
    val item: CatalogOffer,
    val amount: Int,
    val extraData: String,
    val credits: Int,
    val points: Int,
    val pointsType: Int,
)
