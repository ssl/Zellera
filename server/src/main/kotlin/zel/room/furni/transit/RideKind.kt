package zel.room.furni.transit

sealed interface RideKind {
    data object Door : RideKind

    data object Tile : RideKind

    data object Hopper : RideKind
}
