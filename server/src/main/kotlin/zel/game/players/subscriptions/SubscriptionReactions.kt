package zel.game.players.subscriptions

interface SubscriptionReactions {
    fun onStarted(subscription: Subscription) = Unit

    fun onExtended(
        subscription: Subscription,
        bySeconds: Int,
    ) = Unit

    fun onExpired(subscription: Subscription) = Unit

    object None : SubscriptionReactions
}
