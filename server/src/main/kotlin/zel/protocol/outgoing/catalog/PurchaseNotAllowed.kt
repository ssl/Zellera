@file:Suppress("MagicNumber") // the code numbers the client reads

package zel.protocol.outgoing.catalog

enum class PurchaseNotAllowed(
    val code: Int,
) {
    REFUSED(0),
    CLUB_ONLY(1),
}
