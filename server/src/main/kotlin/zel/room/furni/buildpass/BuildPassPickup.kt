package zel.room.furni.buildpass

data class BuildPassPickup(
    val accounts: List<BuildPassAccount>,
    val roomOwnerIsMember: Boolean,
)
