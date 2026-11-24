package zel.room.entry

sealed interface DoorAnswer {
    data object In : DoorAnswer

    data object Banned : DoorAnswer

    data object Ring : DoorAnswer

    data object WrongPassword : DoorAnswer

    data object Invisible : DoorAnswer

    data object BuildPassLocked : DoorAnswer

    data object Queue : DoorAnswer
}
