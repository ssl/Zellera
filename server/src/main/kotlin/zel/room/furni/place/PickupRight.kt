package zel.room.furni.place

sealed interface PickupRight {
    data object Allowed : PickupRight

    data object Refused : PickupRight
}
