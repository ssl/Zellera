package zel.game.catalog

data class OfferPrice(
    val credits: Int,
    val points: Int,
    val pointsType: Int,
) {
    companion object {
        /** Negative amounts read as free. */
        fun stored(
            credits: Int,
            points: Int,
            pointsType: Int,
        ): OfferPrice = OfferPrice(maxOf(0, credits), maxOf(0, points), pointsType)
    }
}
