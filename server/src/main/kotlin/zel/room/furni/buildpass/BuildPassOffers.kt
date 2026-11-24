package zel.room.furni.buildpass

import zel.game.items.ProductKind

fun interface BuildPassOffers {
    /** The offer a player of [rank] may place from a Build Pass page; null for any other offer. */
    fun find(
        pageId: Int,
        offerId: Int,
        sent: String,
        type: ProductKind,
        rank: Int,
    ): BuildPassOffer?
}
