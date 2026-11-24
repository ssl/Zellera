package zel.room.furni.buildpass

data class BuildPassRequest(
    val pageId: Int,
    val offerId: Int,
    val sent: String,
    val spot: BuildPassSpot,
    val confirmed: Boolean,
)
