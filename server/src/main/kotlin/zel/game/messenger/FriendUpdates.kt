package zel.game.messenger

import zel.game.players.Player
import zel.game.players.connection
import zel.protocol.friends.FriendMessages
import zel.protocol.generated.FriendListUpdateOutCodec
import zel.protocol.generated.FriendNotificationOut
import zel.protocol.generated.FriendNotificationOutCodec

class FriendUpdates(
    private val messages: FriendMessages,
) {
    fun friendAdded(
        watcher: Player,
        friend: Friend,
    ) = send(watcher, friend, FriendMessages.ADDED)

    fun friendChanged(
        watcher: Player,
        friend: Friend,
    ) = send(watcher, friend, FriendMessages.CHANGED)

    fun notifyFriend(
        friend: Player,
        aboutPlayerId: Int,
        type: Int,
        data: String,
    ) = friend.connection.send(
        FriendNotificationOutCodec,
        FriendNotificationOut(aboutPlayerId.toString(), type, data),
    )

    private fun send(
        watcher: Player,
        friend: Friend,
        action: Int,
    ) {
        val client = watcher.client ?: return
        client.send(FriendListUpdateOutCodec, messages.friendUpdate(watcher, listOf(friend), action))
    }
}
