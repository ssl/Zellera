package zel.room.furni.place

import zel.room.RoomScope

fun interface PlacedCount {
    /** The room's own owner placed: staff acting as owner never get the counts of a room they don't own. */
    fun placed(
        scope: RoomScope,
        ownerId: Int,
    )

    fun anyPlaced(
        scope: RoomScope,
        placerId: Int,
    ) {}

    fun noteLeft(
        scope: RoomScope,
        writerId: Int,
    ) {}

    companion object {
        val None: PlacedCount = PlacedCount { _, _ -> }
    }
}
