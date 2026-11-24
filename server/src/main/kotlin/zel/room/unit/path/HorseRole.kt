package zel.room.unit.path

sealed interface HorseRole {
    data object None : HorseRole

    data object Ridden : HorseRole
}
