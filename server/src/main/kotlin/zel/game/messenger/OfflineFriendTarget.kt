package zel.game.messenger

data class OfflineFriendTarget(
    val id: Int,
    val rankId: Int,
    val noFriendRequests: Boolean,
    val friendCount: Int,
    val ownMaxFriends: Int,
)
