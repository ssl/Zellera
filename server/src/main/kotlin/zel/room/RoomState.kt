package zel.room

sealed interface RoomState {
    data object Loading : RoomState

    data object Loaded : RoomState

    data object Unloading : RoomState

    data object Unloaded : RoomState

    companion object {
        val order: List<RoomState> = listOf(Loading, Loaded, Unloading, Unloaded)
    }
}
