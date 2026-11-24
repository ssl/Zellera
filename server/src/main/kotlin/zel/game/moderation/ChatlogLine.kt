package zel.game.moderation

/** [timestamp] is unix seconds; [highlighted] marks a line the reporter picked. */
data class ChatlogLine(
    val timestamp: Int,
    val playerId: Int,
    val username: String,
    val message: String,
    val highlighted: Boolean = false,
) {
    fun asHighlighted(): ChatlogLine = copy(highlighted = true)
}
