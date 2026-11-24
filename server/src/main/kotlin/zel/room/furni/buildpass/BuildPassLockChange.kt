package zel.room.furni.buildpass

sealed interface BuildPassLockChange {
    data object None : BuildPassLockChange

    data object Locked : BuildPassLockChange

    data object Unlocked : BuildPassLockChange
}
