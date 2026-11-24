package zel.room.entry

import zel.room.RoomScope
import zel.room.map.Point

fun interface ArrivalSpots {
    fun spotOf(
        scope: RoomScope,
        playerId: Int,
    ): ArrivalSpot?

    companion object {
        val None = ArrivalSpots { _, _ -> null }
    }
}

/** [z] null: the tile's walk height. */
data class ArrivalSpot(
    val tile: Point,
    val direction: Int,
    val z: Double? = null,
)
