package zel.game.messenger

import zel.core.threading.GameThreads

class Friendships(
    private val store: FriendStore,
    private val connected: ConnectedPlayers,
    private val updates: FriendUpdates,
    private val threading: GameThreads,
) {
    /** [accepterId] takes [requesterId]'s request; false when there was none. Both lists learn of the other. */
    fun accept(
        requesterId: Int,
        accepterId: Int,
    ): Boolean {
        if (!store.acceptRequest(requesterId, accepterId)) return false
        listOf(accepterId to requesterId, requesterId to accepterId).forEach { (ownerId, friendId) ->
            val owner = connected.byId(ownerId) ?: return@forEach
            owner.messenger.loadFriend(ownerId, friendId)?.let { updates.friendAdded(owner, it) }
        }
        return true
    }

    /** Sets the owner's relation to [friend]; any value but 1..3 clears it. */
    fun setRelation(
        friend: Friend,
        relation: Int,
    ) {
        friend.relation = (if (relation in RELATIONS) relation else 0).toShort()
        threading.run { store.saveRelation(friend) }
    }

    private companion object {
        val RELATIONS = 1..3
    }
}
