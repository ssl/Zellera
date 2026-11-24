package zel.game.moderation

/** [timestamp] is unix seconds. */
data class RoomVisit(
    val roomId: Int,
    val roomName: String,
    val timestamp: Int,
    val chat: List<ChatlogLine> = emptyList(),
)
