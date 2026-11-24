package zel.room.furni.buildpass

data class BuildPassAccount(
    val playerId: Int,
    val furniCount: Int,
    val secondsLeft: Int,
    val limit: Int,
    val maxLimit: Int,
) {
    val isMember: Boolean get() = secondsLeft > 0

    val isFull: Boolean get() = furniCount >= limit
}
