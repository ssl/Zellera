package zel.game.catalog

import zel.util.SECONDS_PER_DAY

/** Price arithmetic that refuses to overflow or go negative instead of wrapping. */
object Prices {
    fun total(
        unit: Int,
        count: Int,
    ): Int = fits(nonNegative(unit, "price").toLong() * nonNegative(count, "count"), "price")

    fun sum(
        a: Int,
        b: Int,
    ): Int = fits(nonNegative(a, "price").toLong() + nonNegative(b, "price"), "price")

    fun subscriptionSeconds(days: Int): Int = fits(nonNegative(days, "days").toLong() * SECONDS_PER_DAY, "days")

    /** The seconds a membership of [days] lasts; refused with a [price] no purse can be charged. */
    fun membershipSeconds(
        price: OfferPrice,
        days: Int,
    ): Int {
        nonNegative(price.credits, "price")
        nonNegative(price.points, "price")
        return subscriptionSeconds(days)
    }

    private fun nonNegative(
        value: Int,
        what: String,
    ): Int {
        require(value >= 0) { "$what is negative: $value" }
        return value
    }

    private fun fits(
        value: Long,
        what: String,
    ): Int {
        require(value <= Int.MAX_VALUE) { "$what does not fit: $value" }
        return value.toInt()
    }
}
