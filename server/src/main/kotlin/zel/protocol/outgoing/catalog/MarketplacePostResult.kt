@file:Suppress("MagicNumber") // the code numbers the client reads

package zel.protocol.outgoing.catalog

enum class MarketplacePostResult(
    val code: Int,
) {
    POSTED(1),
    FAILED(2),
    CLOSED(3),
    ALREADY_LISTED(4),
}
