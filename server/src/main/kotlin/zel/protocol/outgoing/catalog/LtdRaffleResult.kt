@file:Suppress("MagicNumber") // the code numbers the client reads

package zel.protocol.outgoing.catalog

enum class LtdRaffleResult(
    val code: Int,
) {
    WON(0),
    LOST(1),
    CANCELLED(2),
    ERROR(3),
}
