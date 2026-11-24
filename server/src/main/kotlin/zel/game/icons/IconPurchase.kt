package zel.game.icons

/** [offerId] and [localizationId] go into PurchaseOK; a set's carries offer 0. */
data class IconPurchase(
    val offerId: Int,
    val localizationId: String,
    val price: IconRoster.Price,
    val ids: List<Int>,
)
