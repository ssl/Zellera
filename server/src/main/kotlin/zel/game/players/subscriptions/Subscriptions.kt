package zel.game.players.subscriptions

import zel.server.GameServices

class Subscriptions(
    private val services: GameServices,
) {
    internal val store = SubscriptionStore(services)
    private val club = ClubReactions(services)
    private val buildPass = SubscriptionBuildPass(services)

    /** Null when it could not be stored; the product reacts once it is in the list. */
    fun open(
        userId: Int,
        type: String,
        startedAt: Int,
        duration: Int,
    ): Subscription? {
        val id = store.insert(userId, type, startedAt, duration) ?: return null
        val row = SubscriptionRow(id, userId, type, startedAt, duration)
        return Subscription(row, store, services::unixTimestamp, reactionsOf(type))
    }

    fun ofPlayer(userId: Int): MutableSet<Subscription> =
        store.read(userId).mapTo(HashSet()) { row ->
            val reactions = reactionsOf(row.type)
            Subscription(row, store, services::unixTimestamp, reactions)
        }

    private fun reactionsOf(type: String): SubscriptionReactions =
        when (type) {
            Subscription.CLUB -> club
            Subscription.BUILD_PASS -> buildPass
            else -> SubscriptionReactions.None
        }
}
