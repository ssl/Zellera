package zel.wired.chest

data class PendingNotice(
    val ownerId: Int,
    val chestId: Int,
    val roomId: Int,
    val reason: Int,
    val chestName: String,
    val actorId: Int,
    val createdAt: Int,
)
