package zel.room.furni.buildpass

import zel.room.furni.place.FurniAccess

data class BuildPassPlacer(
    val access: FurniAccess,
    val hasClub: Boolean,
    val name: String,
    val own: BuildPassAccount,
    val pool: BuildPassAccount?,
    val poolName: String,
    val admins: List<BuildPassAccount>,
    val roomOwnerIsMember: Boolean,
)
