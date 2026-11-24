package zel.game.messenger

import zel.game.players.Player
import java.util.concurrent.ConcurrentHashMap

class FriendList(
    private val store: FriendStore,
    private val connected: ConnectedPlayers,
    private val updates: FriendUpdates,
) {
    val friends: ConcurrentHashMap<Int, Friend> = ConcurrentHashMap()

    fun load(owner: Player) {
        val ownerId = owner.profile.id
        store.friendsOf(ownerId).forEach { friends.putIfAbsent(it.id, it) }
    }

    fun friend(id: Int): Friend? = friends[id]

    fun remove(id: Int) {
        friends.remove(id)
    }

    internal fun loadFriend(
        ownerId: Int,
        friendId: Int,
    ): Friend? = store.friendOf(ownerId, friendId)?.let { friends.putIfAbsent(it.id, it) ?: it }

    fun matching(text: String): Set<Friend> =
        friends.values.filterTo(HashSet()) { it.username.contains(text, ignoreCase = true) }

    fun connectionChanged(
        owner: Player,
        online: Boolean,
        inRoom: Boolean,
    ) {
        val visibleOnline = online && !owner.stats.settings.appearOffline
        val visibleInRoom = visibleOnline && inRoom
        friends.values
            .asSequence()
            .filter { it.isOnline }
            .mapNotNull { connected.byId(it.id) }
            .forEach { watcher ->
                watcher.messenger.friend(owner.profile.id)?.let { entry ->
                    entry.learn(owner, visibleOnline, visibleInRoom)
                    updates.friendChanged(watcher, entry)
                }
            }
    }

    fun dispose() {
        friends.clear()
    }
}
