package zel.room.furni.buildpass

data class BuildPassOwnedRoom(
    val id: Int,
    val locked: Boolean,
    val hidden: Boolean,
    val hasFurni: Boolean,
)
