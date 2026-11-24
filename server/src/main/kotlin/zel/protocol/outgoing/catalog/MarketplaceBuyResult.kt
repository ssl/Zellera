@file:Suppress("MagicNumber") // the code numbers the client reads

package zel.protocol.outgoing.catalog

enum class MarketplaceBuyResult(
    val code: Int,
) {
    BOUGHT(1),
    GONE(2),
    OFFER_CHANGED(3),
    NO_CREDITS(4),
}
