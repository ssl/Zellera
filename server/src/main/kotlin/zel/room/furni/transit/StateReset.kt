package zel.room.furni.transit

data class StateReset(
    val due: Long,
    val furniId: Int,
    val state: String,
)
