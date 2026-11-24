@file:Suppress("MagicNumber") // the code numbers the client reads

package zel.protocol.outgoing.catalog

enum class PlacementKind(
    val code: Int,
) {
    FLOOR(0),
    WALL(1),
}
