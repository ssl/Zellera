package zel.game.players.subscriptions

/** [duration] is in seconds. */
data class SubscriptionRow(
    val id: Int,
    val playerId: Int,
    val type: String,
    val startedAt: Int,
    val duration: Int,
    val active: Boolean = true,
)
