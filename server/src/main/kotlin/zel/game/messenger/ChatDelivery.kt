package zel.game.messenger

import zel.core.Configuration
import zel.core.threading.GameThreads
import zel.game.moderation.WordFilter
import zel.game.players.Player
import zel.net.clients.ClientConnection
import zel.protocol.friends.FriendMessages
import zel.protocol.generated.NewConsoleOut
import zel.protocol.generated.NewConsoleOutCodec

class ChatDelivery(
    private val configuration: Configuration,
    private val clock: () -> Int,
    private val threading: GameThreads,
    private val connected: ConnectedPlayers,
    private val wordFilter: WordFilter,
    private val messages: FriendMessages,
    private val history: MessengerHistory,
    private val inbox: OfflineInbox,
) {
    fun sendText(
        friend: Friend,
        from: Player,
        text: String,
    ): Message? {
        val filtered = configuration.getBoolean("word_filter.covers_messenger")
        val shown = if (filtered) wordFilter.filter(text, from) else text
        return route(Message(from.profile.id, friend.id, shown, clock()))
    }

    fun sendIcon(
        friend: Friend,
        from: Player,
        iconId: Int,
    ): Message? = route(Message(from.profile.id, friend.id, "", clock(), iconId))

    fun show(
        client: ClientConnection,
        message: NewConsoleOut,
    ) = client.send(NewConsoleOutCodec, message)

    /** Shows what waited (from players who are still friends) and marks it delivered. */
    fun deliverInbox(recipient: Player) {
        val waiting = inbox.waitingFor(recipient.profile.id)
        if (waiting.isNullOrEmpty()) return
        recipient.client?.let { client ->
            waiting
                .map { it.message }
                .filter { recipient.messenger.friends.containsKey(it.fromId) }
                .forEach { show(client, messages.chat(it)) }
        }
        inbox.handedOver(recipient.profile.id, waiting)
    }

    /** Null when the recipient is away and the message cannot wait for them. */
    private fun route(message: Message): Message? {
        val client = reachableClient(message.toId)
        if (client == null) {
            val waits = inbox.hasRoomFor(message.fromId, message.toId) && history.store(message, delivered = false)
            return message.takeIf { waits }
        }
        threading.write { history.store(message, delivered = true) }
        show(client, messages.chat(message))
        return message
    }

    private fun reachableClient(playerId: Int): ClientConnection? {
        val player = connected.byId(playerId)?.takeIf { it.isOnline } ?: return null
        return player.client?.takeIf { it.channel.isOpen }
    }
}
