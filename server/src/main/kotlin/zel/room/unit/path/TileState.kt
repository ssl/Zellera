package zel.room.unit.path

import zel.room.furni.RoomFurni

sealed interface TileState {
    fun isTransit(): Boolean = this is Restricted && (passage is Passage.Transit || passage is Passage.Nest)

    data object Void : TileState

    data object Blocked : TileState

    data object Open : TileState

    /** A chair: a walk's goal, never passed. */
    data class Seat(
        val furni: RoomFurni,
    ) : TileState

    /** A bed: a walk's goal for lying, never passed. */
    data class Bed(
        val furni: RoomFurni,
    ) : TileState

    data class Restricted(
        val passage: Passage,
    ) : TileState
}
