package zel.game.catalog

/** What a code gives. How often it may still be used is only in the database. */
data class VoucherCode(
    val id: Int,
    val credits: Int,
    val points: Int,
    val pointsType: Int,
    val offerId: Int,
)
