package zel.protocol.navigator

import zel.room.listing.ListedRoom

sealed interface RoomCreationResult {
    data class LimitReached(
        val count: Int,
        val max: Int,
    ) : RoomCreationResult

    data class Created(
        val room: ListedRoom,
    ) : RoomCreationResult

    data object Refused : RoomCreationResult
}
