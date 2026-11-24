package zel.game.catalog

import zel.game.items.CreditFurni
import zel.game.items.Item

internal object OfferExtraParams {
    fun shownOnPage(
        item: Item,
        productParam: String,
    ): String = CreditFurni.credits(item).takeIf { it > 0 }?.toString() ?: productParam
}
