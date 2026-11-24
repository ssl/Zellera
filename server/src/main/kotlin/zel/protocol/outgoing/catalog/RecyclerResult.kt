@file:Suppress("MagicNumber") // the code numbers the client reads

package zel.protocol.outgoing.catalog

enum class RecyclerResult(
    val code: Int,
) {
    DONE(1),
    CLOSED(2),
}
