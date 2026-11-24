package zel.protocol.friends

import zel.game.messenger.Friend
import zel.game.messenger.FriendRequest
import zel.game.messenger.Message
import zel.game.messenger.MessengerHistory
import zel.game.permissions.PlayerLimits
import zel.game.permissions.Privilege
import zel.game.players.Gender
import zel.game.players.Player
import zel.game.players.Players
import zel.protocol.generated.AcceptFriendResultOut
import zel.protocol.generated.ConsoleMessageHistoryOut
import zel.protocol.generated.FriendListFragmentOut
import zel.protocol.generated.FriendListUpdateOut
import zel.protocol.generated.FriendRequestsOut
import zel.protocol.generated.MessengerErrorOut
import zel.protocol.generated.MessengerInitOut
import zel.protocol.generated.NewConsoleOut
import zel.protocol.generated.PacketWriter
import zel.protocol.generated.PlayerSearchResultOut

@Suppress("TooManyFunctions") // one builder per message
class FriendMessages(
    private val players: Players,
    private val clock: () -> Int,
) {
    fun messengerInit(player: Player): MessengerInitOut {
        val categories =
            player.profile.friendCategories
                .map { MessengerInitOut.List1Item(it.id, it.name) }
        val normalLimit = PlayerLimits.maxFriendsShown(player, false)
        return if (player.hasPermission(Privilege.FRIENDS_UNLIMITED)) {
            MessengerInitOut(Int.MAX_VALUE, normalLimit, Int.MAX_VALUE, categories)
        } else {
            MessengerInitOut(
                PlayerLimits.maxFriends(player),
                normalLimit,
                PlayerLimits.maxFriendsShown(player, true),
                categories,
            )
        }
    }

    /** An empty list still sends one empty page. */
    fun friendPages(list: Collection<Friend>): List<FriendListFragmentOut> {
        val chunks = list.chunked(PAGE_SIZE).ifEmpty { listOf(emptyList()) }
        val pageCount = if (list.isEmpty()) 0 else chunks.size
        return chunks.mapIndexed { at, chunk -> FriendListFragmentOut(pageCount, at, chunk.map(::listed)) }
    }

    private fun listed(friend: Friend) =
        FriendListFragmentOut.List1Item(
            friend.id,
            friend.username,
            genderCode(friend),
            friend.shownOnline,
            friend.shownInRoom,
            friend.look,
            friend.categoryId,
            friend.motto,
            "",
            "",
            true,
            false,
            false,
            friend.relation,
        )

    private fun PacketWriter.friendFields(friend: Friend) {
        int(friend.id)
        string(friend.username)
        int(genderCode(friend))
        boolean(friend.shownOnline)
        boolean(friend.shownInRoom)
        string(friend.look)
        int(friend.categoryId)
        string(friend.motto)
        string("")
        string("")
        boolean(true)
        boolean(false)
        boolean(false)
        short(friend.relation)
    }

    fun friendUpdate(
        player: Player?,
        list: Collection<Friend>,
        change: Int,
    ): FriendListUpdateOut {
        val categories = player?.profile?.friendCategories.orEmpty()
        val body = PacketWriter()
        body.int(list.size)
        list.forEach { friend ->
            body.int(change)
            body.friendFields(friend)
        }
        return FriendListUpdateOut(
            categories.map { FriendListUpdateOut.List1Item(it.id, it.name) },
            body.toByteArray(),
        )
    }

    fun friendsRemoved(ids: Collection<Int>): FriendListUpdateOut {
        val body = PacketWriter()
        body.int(ids.size)
        ids.forEach { id ->
            body.int(REMOVED)
            body.int(id)
        }
        return FriendListUpdateOut(emptyList(), body.toByteArray())
    }

    fun requestError(code: Int): MessengerErrorOut = MessengerErrorOut(FRIEND_REQUEST_CONTEXT, code)

    fun friendRequests(requests: Collection<FriendRequest>): FriendRequestsOut =
        FriendRequestsOut(
            requests.size,
            requests.map { FriendRequestsOut.List1Item(it.id, it.username, it.look) },
        )

    fun acceptFailed(
        userIds: List<Int>,
        errorCode: Int,
    ): AcceptFriendResultOut = AcceptFriendResultOut(userIds.map { AcceptFriendResultOut.List1Item(it, errorCode) })

    fun searchResult(
        found: List<Friend>,
        friends: Set<Friend>,
    ): PlayerSearchResultOut {
        val friendNames = friends.mapTo(HashSet()) { it.username }
        val strangers = found.filter { it.username !in friendNames }.sortedWith(NAME_ORDER)
        return PlayerSearchResultOut(
            friends.map {
                PlayerSearchResultOut.List1Item(
                    it.id,
                    it.username,
                    it.motto,
                    false,
                    false,
                    "",
                    1,
                    it.look,
                    "",
                )
            },
            strangers.map {
                val look = if (it.shownOnline) it.look else ""
                PlayerSearchResultOut.List2Item(it.id, it.username, it.motto, false, false, "", 1, look, "")
            },
        )
    }

    fun chat(message: Message): NewConsoleOut = chat(message, message.fromId, message.fromId, 0)

    /** A sender whose profile cannot be read shows without a name and a look. */
    fun chat(
        message: Message,
        chatId: Int,
        senderId: Int,
        confirmationId: Int,
    ): NewConsoleOut {
        val sender = if (senderId > 0) players.profileOf(senderId) else null
        val name = sender?.name.orEmpty()
        val look = sender?.figure.orEmpty()
        val kind = if (message.iconId > 0) CONTENT_ICON else CONTENT_TEXT
        val body = PacketWriter()
        if (kind == CONTENT_ICON) body.int(message.iconId) else body.string(message.message)
        body.int(clock() - message.timestamp)
        body.string(message.id)
        body.int(confirmationId)
        body.int(senderId)
        body.string(name)
        body.string(look)
        return NewConsoleOut(chatId, kind, body.toByteArray())
    }

    fun history(
        chatId: Int,
        lines: List<MessengerHistory.Entry>,
    ): ConsoleMessageHistoryOut {
        val now = clock()
        val body = PacketWriter()
        body.int(lines.size)
        lines.forEach { line ->
            body.int(line.senderId)
            body.string(line.senderName)
            body.string(line.senderFigure)
            if (line.iconId > 0) {
                body.int(CONTENT_ICON)
                body.int(line.iconId)
            } else {
                body.int(CONTENT_TEXT)
                body.string(line.text)
            }
            body.int(maxOf(0, now - line.timestamp))
            body.string(line.messageId)
        }
        return ConsoleMessageHistoryOut(chatId, body.toByteArray())
    }

    private fun genderCode(friend: Friend) = if (friend.gender == Gender.M) 0 else 1

    companion object {
        const val PAGE_SIZE = 750

        const val REMOVED = -1
        const val CHANGED = 0
        const val ADDED = 1
        private const val FRIEND_REQUEST_CONTEXT = 39
        private const val CONTENT_TEXT = 0
        private const val CONTENT_ICON = 1
        private val NAME_ORDER =
            compareBy<Friend> { it.username.length }.thenComparator { a, b ->
                a.username.compareTo(b.username, ignoreCase = true)
            }
    }
}
