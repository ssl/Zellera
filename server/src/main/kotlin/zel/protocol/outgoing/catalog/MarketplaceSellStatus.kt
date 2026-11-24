@file:Suppress("MagicNumber") // the code numbers the client reads

package zel.protocol.outgoing.catalog

enum class MarketplaceSellStatus(
    val code: Int,
) {
    OPEN(1),

    /** The client shows "not allowed to make a marketplace offer": a closed marketplace, a trade lock. */
    CLOSED(2),
    NO_TRADE_PASS(3),

    NO_TOKENS(4),
}
