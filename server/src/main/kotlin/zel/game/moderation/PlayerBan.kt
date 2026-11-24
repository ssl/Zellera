package zel.game.moderation

/** A ban on a player; times are unix seconds, [issuedBy] and [topicId] 0 for none. */
data class PlayerBan(
    val playerId: Int,
    val issuedBy: Int,
    val issuedAt: Int,
    val endsAt: Int,
    val reason: String,
    val topicId: Int,
)
