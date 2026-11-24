@file:Suppress("MagicNumber") // the code numbers the client reads

package zel.protocol.outgoing.catalog

enum class VoucherError(
    val code: Int,
) {
    UNKNOWN_CODE(0),
    TECHNICAL(1),
}
