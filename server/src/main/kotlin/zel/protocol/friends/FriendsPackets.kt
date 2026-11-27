package zel.protocol.friends

import org.slf4j.LoggerFactory
import zel.db.SqlFailure
import zel.game.achievements.AchievementTally
import zel.game.icons.IconAchievements
import zel.game.icons.IconService
import zel.game.messenger.Friend
import zel.game.messenger.MeetingRoom
import zel.game.messenger.MessengerModule
import zel.game.messenger.OfflineFriendTarget
import zel.game.moderation.WordFilter
import zel.game.permissions.PlayerLimits
import zel.game.permissions.Privilege
import zel.game.players.Player
import zel.game.players.Players
import zel.game.players.connection
import zel.game.quests.CountedAction
import zel.game.quests.PlayerActions
import zel.protocol.generated.AcceptFriendInCodec
import zel.protocol.generated.AcceptFriendResultOutCodec
import zel.protocol.generated.ConsoleMessageHistoryOutCodec
import zel.protocol.generated.DeclineFriendInCodec
import zel.protocol.generated.FindFriendsProcessResultOut
import zel.protocol.generated.FindFriendsProcessResultOutCodec
import zel.protocol.generated.FindNewFriendsInCodec
import zel.protocol.generated.FollowFriendFailedOut
import zel.protocol.generated.FollowFriendFailedOutCodec
import zel.protocol.generated.FollowFriendInCodec
import zel.protocol.generated.FriendListFragmentOutCodec
import zel.protocol.generated.FriendListUpdateInCodec
import zel.protocol.generated.FriendListUpdateOutCodec
import zel.protocol.generated.FriendRequestsOutCodec
import zel.protocol.generated.GetFriendRequestsInCodec
import zel.protocol.generated.GetMOTDInCodec
import zel.protocol.generated.GetMessengerHistoryInCodec
import zel.protocol.generated.InstantMessageErrorOut
import zel.protocol.generated.InstantMessageErrorOutCodec
import zel.protocol.generated.MessengerErrorOutCodec
import zel.protocol.generated.MessengerInitInCodec
import zel.protocol.generated.MessengerInitOutCodec
import zel.protocol.generated.NewFriendRequestOut
import zel.protocol.generated.NewFriendRequestOutCodec
import zel.protocol.generated.PlayerSearchInCodec
import zel.protocol.generated.PlayerSearchResultOutCodec
import zel.protocol.generated.RemoveFriendInCodec
import zel.protocol.generated.RequestFriendInCodec
import zel.protocol.generated.RoomForwardOut
import zel.protocol.generated.RoomForwardOutCodec
import zel.protocol.generated.RoomInviteErrorOut
import zel.protocol.generated.RoomInviteErrorOutCodec
import zel.protocol.generated.RoomInviteOut
import zel.protocol.generated.RoomInviteOutCodec
import zel.protocol.generated.SendIconInCodec
import zel.protocol.generated.SendMsgInCodec
import zel.protocol.generated.SendRoomInviteInCodec
import zel.protocol.generated.SetRelationshipStatusInCodec
import zel.protocol.generated.VisitPlayerInCodec
import zel.protocol.outgoing.handshake.ErrorReport
import zel.protocol.typed.ClientCooldown
import zel.protocol.typed.PacketPackage
import zel.protocol.typed.PacketRoute
import zel.protocol.typed.PacketSession
import zel.protocol.typed.player
import zel.room.listing.RoomListings
import zel.room.port.RoomPort
import java.util.concurrent.ThreadLocalRandom

// one guard clause per refusal the messenger answers, and one handler per message
@Suppress("ReturnCount", "LoopWithTooManyJumpStatements", "TooManyFunctions")
class FriendsPackets(
    private val messenger: MessengerModule,
    private val players: Players,
    private val rooms: RoomListings,
    private val wordFilter: WordFilter,
    private val live: () -> RoomPort,
) : PacketPackage {
    override val name = "friends"

    private val friendships = messenger.friendships
    private val requests = messenger.requests
    private val chat = messenger.chat
    private val listAchievement = messenger.listAchievement
    private val messages = messenger.messages
    private val connected = messenger.connected
    private val refreshCooldown = ClientCooldown("friends.refresh", FriendPacing.LIST_REFRESH)
    private val requestCooldown = ClientCooldown("friends.request", FriendPacing.LIST_CHANGE)
    private val acceptCooldown = ClientCooldown("friends.accept", FriendPacing.LIST_CHANGE)
    private val declineCooldown = ClientCooldown("friends.decline", FriendPacing.LIST_CHANGE)
    private val removeCooldown = ClientCooldown("friends.remove", FriendPacing.LIST_CHANGE)
    private val searchCooldown = ClientCooldown("friends.search", FriendPacing.SEARCH)
    private val historyCooldown = ClientCooldown("friends.history", FriendPacing.LOOKUP)
    private val followCooldown = ClientCooldown("friends.follow", FriendPacing.LOOKUP)
    private val findCooldown = ClientCooldown("friends.find", FriendPacing.SEARCH)
    private val inviteCooldown = ClientCooldown("friends.invite", FriendPacing.INVITE)

    override val routes =
        listOf(
            PacketRoute(MessengerInitInCodec) { session, _ -> messengerInit(session) },
            PacketRoute(GetMOTDInCodec) { _, _ -> },
            PacketRoute(FriendListUpdateInCodec) { session, _ -> refreshFriendList(session) },
            PacketRoute(RequestFriendInCodec) { session, m -> requestFriend(session, m.username) },
            PacketRoute(AcceptFriendInCodec) { session, m -> acceptFriends(session, m.userIds) },
            PacketRoute(
                DeclineFriendInCodec,
            ) { session, m -> declineFriends(session, m.declineAll, m.userIds) },
            PacketRoute(GetFriendRequestsInCodec) { session, _ -> friendRequests(session) },
            PacketRoute(RemoveFriendInCodec) { session, m -> removeFriends(session, m.userIds) },
            PacketRoute(
                SetRelationshipStatusInCodec,
            ) { session, m -> changeRelation(session, m.userId, m.relationId) },
            PacketRoute(PlayerSearchInCodec) { session, m -> searchUsers(session, m.username) },
            PacketRoute(
                SendMsgInCodec,
            ) { session, m -> privateMessage(session, m.userId, m.message, m.confirmationId) },
            PacketRoute(
                SendIconInCodec,
            ) { session, m -> iconMessage(session, m.friendId, m.iconId, m.trackingId) },
            PacketRoute(GetMessengerHistoryInCodec) { session, m -> history(session, m.chatId, m.before) },
            PacketRoute(
                SendRoomInviteInCodec,
            ) { session, m -> inviteFriends(session, m.userIds, m.message) },
            PacketRoute(FindNewFriendsInCodec) { session, _ -> findNewFriends(session) },
            PacketRoute(FollowFriendInCodec) { session, m -> follow(session, m.int1) },
            PacketRoute(VisitPlayerInCodec) { session, m -> visitUser(session, m.username) },
        )

    private fun messengerInit(session: PacketSession) {
        if (!session.client.startMessengerOnce()) return
        val player = session.player
        session.send(MessengerInitOutCodec, messages.messengerInit(player))
        messages
            .friendPages(
                player.messenger.friends.values,
            ).forEach { session.send(FriendListFragmentOutCodec, it) }
        listAchievement.listSent(player)
        chat.deliverInbox(player)
    }

    private fun refreshFriendList(session: PacketSession) {
        if (!refreshCooldown.acquire(session.client)) return
        val player = session.player
        session.send(
            FriendListUpdateOutCodec,
            messages.friendUpdate(player, player.messenger.friends.values, FriendMessages.CHANGED),
        )
    }

    private fun requestFriend(
        session: PacketSession,
        rawName: String,
    ) {
        if (!requestCooldown.acquire(session.client)) return
        val username = FriendInput.name(rawName) ?: return
        val player = session.player
        val ownId = player.profile.id
        if (username.equals(player.profile.name, ignoreCase = true)) return
        if (friendByName(player.messenger.friends.values, username) != null) return
        if (listFull(player)) return session.sendRequestError(FRIEND_LIST_OWN_FULL)

        val target =
            connected.byName(username)?.let(::target) ?: try {
                messenger.offlinePlayers.byName(username)?.let { target(it, player) }
            } catch (e: SqlFailure) {
                LOGGER.error("Friend request: looking up {} failed", username, e)
                return
            }
        val refusal =
            when {
                target == null -> TARGET_NOT_FOUND

                player.stats.contacts.userBlocked(target.id) -> TARGET_BLOCKED_BY_ASKER

                // A player who blocked the asker answers like one who takes no requests at all.
                target.noFriendRequests || messenger.blocks.blocks(target.id, ownId) -> TARGET_NOT_ACCEPTING_REQUESTS

                target.listFull -> FRIEND_LIST_TARGET_FULL

                else -> null
            }
        if (target == null || refusal != null) return session.sendRequestError(refusal ?: TARGET_NOT_FOUND)

        val between = requests.between(ownId, target.id)
        when {
            // a request waits either way: the one who was asked answers it
            between.askedBy(target.id) || between.askedBy(ownId) -> Unit

            else -> ask(session, target)
        }
    }

    private fun ask(
        session: PacketSession,
        target: Target,
    ) {
        val player = session.player
        val profile = player.profile
        val refusal =
            when {
                !hasRoomToAsk(player) -> FRIEND_LIST_OWN_FULL
                requests.waitingCount(target.id) >= target.maxFriends -> TARGET_NOT_ACCEPTING_REQUESTS
                else -> null
            }
        if (refusal != null) return session.sendRequestError(refusal)
        if (!requests.add(profile.id, target.id)) return
        target.online?.takeIf { it.isOnline }?.connection?.send(
            NewFriendRequestOutCodec,
            NewFriendRequestOut(profile.id, profile.name, profile.figure),
        )
        PlayerActions.progress(player, CountedAction.REQUEST_FRIEND, 1)
    }

    /** Friends and unanswered requests together never exceed the list: more could not all be accepted. */
    private fun hasRoomToAsk(player: Player): Boolean =
        player.hasPermission(Privilege.FRIENDS_UNLIMITED) ||
            player.messenger.friends.size + requests.sentBy(player.profile.id) < PlayerLimits.maxFriends(player)

    private class Target(
        val id: Int,
        val noFriendRequests: Boolean,
        val listFull: Boolean,
        val maxFriends: Int,
        val online: Player?,
    )

    private fun target(online: Player) =
        Target(
            online.profile.id,
            online.stats.settings.noFriendRequests,
            listFull(online),
            PlayerLimits.maxFriends(online),
            online,
        )

    /** Their friend list achievement catches up when they next log in. */
    private fun target(
        offline: OfflineFriendTarget,
        viewer: Player,
    ): Target {
        val permissions = viewer.services.game.permissions
        val max = PlayerLimits.maxFriends(offline.ownMaxFriends, permissions?.ranks?.get(offline.rankId))
        val unlimited = permissions?.allows(offline.rankId, Privilege.FRIENDS_UNLIMITED.key) == true
        return Target(offline.id, offline.noFriendRequests, offline.friendCount >= max && !unlimited, max, null)
    }

    private fun listFull(player: Player) =
        player.messenger.friends.size >= PlayerLimits.maxFriends(player) &&
            !player.hasPermission(Privilege.FRIENDS_UNLIMITED)

    private fun PacketSession.sendRequestError(code: Int) = send(MessengerErrorOutCodec, messages.requestError(code))

    private fun acceptFriends(
        session: PacketSession,
        userIds: List<Int>,
    ) {
        if (!acceptCooldown.acquire(session.client)) return
        val player = session.player
        val ownId = player.profile.id
        val chosen = FriendInput.batch(userIds, PlayerLimits.maxFriends(player))
        if (chosen.isEmpty()) return
        val asked = requests.waitingFor(ownId).mapTo(HashSet()) { it.id }
        val failed = chosen.filterNotTo(ArrayList()) { it in asked }

        for (userId in chosen) {
            if (userId !in asked) continue
            if (player.messenger.friends.containsKey(userId)) {
                requests.decline(listOf(userId), ownId)
                continue
            }
            if (listFull(player)) {
                session.sendRequestError(FRIEND_LIST_OWN_FULL)
                break
            }
            val target = acceptTarget(userId, player)
            if (target?.listFull == true) {
                session.sendRequestError(FRIEND_LIST_TARGET_FULL)
                continue
            }
            if (target == null || !friendships.accept(userId, ownId)) {
                failed.add(userId)
                continue
            }
            listAchievement.friendCountChanged(player)
            target.online?.let(listAchievement::friendCountChanged)
        }

        if (failed.isNotEmpty()) {
            session.send(FriendListUpdateOutCodec, messages.friendUpdate(null, emptyList(), FriendMessages.CHANGED))
            session.send(AcceptFriendResultOutCodec, messages.acceptFailed(failed, NO_SUCH_REQUEST))
        }
    }

    private fun acceptTarget(
        userId: Int,
        player: Player,
    ): Target? =
        players.find(userId)?.let(::target) ?: try {
            messenger.offlinePlayers.byId(userId)?.let { target(it, player) }
        } catch (e: SqlFailure) {
            LOGGER.error("Looking up friend request sender {} failed", userId, e)
            null
        }

    private fun declineFriends(
        session: PacketSession,
        all: Boolean,
        userIds: List<Int>,
    ) {
        if (!declineCooldown.acquire(session.client)) return
        val player = session.player
        val ownId = player.profile.id
        if (all) {
            requests.declineAll(ownId)
        } else {
            requests.decline(FriendInput.batch(userIds, PlayerLimits.maxFriends(player)), ownId)
        }
    }

    private fun friendRequests(session: PacketSession) {
        session.send(FriendRequestsOutCodec, messages.friendRequests(requests.waitingFor(session.player.profile.id)))
    }

    private fun removeFriends(
        session: PacketSession,
        userIds: List<Int>,
    ) {
        if (!removeCooldown.acquire(session.client)) return
        val player = session.player
        val ownId = player.profile.id
        val removed =
            FriendInput
                .batch(userIds, PlayerLimits.maxFriends(player))
                .filter { player.messenger.friends.containsKey(it) }
        if (removed.isEmpty()) return
        for (friendId in removed) {
            messenger.friends.unfriend(ownId, friendId)
            player.messenger.remove(friendId)
            val other = connected.byId(friendId) ?: continue
            other.messenger.remove(ownId)
            other.connection.send(FriendListUpdateOutCodec, messages.friendsRemoved(listOf(ownId)))
        }
        session.send(FriendListUpdateOutCodec, messages.friendsRemoved(removed))
    }

    private fun changeRelation(
        session: PacketSession,
        userId: Int,
        relationId: Int,
    ) {
        if (userId <= 0) return
        val player = session.player
        val friend = player.messenger.friend(userId)
        if (friend != null && FriendInput.isRelation(relationId)) {
            val changed = relationId > 0 && relationId != friend.relation.toInt()
            friendships.setRelation(friend, relationId)
            session.send(
                FriendListUpdateOutCodec,
                messages.friendUpdate(player, listOf(friend), FriendMessages.CHANGED),
            )
            if (changed) PlayerActions.progress(player, CountedAction.SET_RELATIONSHIP_STATUS, 1)
        }
    }

    private fun searchUsers(
        session: PacketSession,
        rawName: String,
    ) {
        if (!searchCooldown.acquire(session.client)) return
        val player = session.player
        val text = FriendInput.searchText(rawName)
        if (text.isEmpty()) return
        session.send(
            PlayerSearchResultOutCodec,
            messages.searchResult(messenger.search.find(text), player.messenger.matching(text)),
        )
    }

    private fun privateMessage(
        session: PacketSession,
        userId: Int,
        rawMessage: String,
        confirmationId: Int,
    ) {
        val message = FriendInput.chatText(rawMessage)
        if (userId <= 0 || message.isEmpty()) return
        val player = session.player
        val friend = chatPartner(session, userId, message) ?: return
        PlayerActions.progress(player, CountedAction.SEND_MESSENGER_MESSAGE, 1)

        val delivered =
            chat.sendText(friend, player, message)
                ?: return refuse(session, userId, InstantMessageRefusal.UNDELIVERED, message)
        chat.show(session.client, messages.chat(delivered, userId, delivered.fromId, confirmationId))
    }

    private fun iconMessage(
        session: PacketSession,
        friendId: Int,
        iconId: Int,
        trackingId: Int,
    ) {
        val player = session.player
        if (friendId <= 0 || iconId <= 0) return
        val friend = chatPartner(session, friendId, "") ?: return
        val senderId = player.profile.id
        if (!player.iconService.use(senderId, IconService.allUnlocked(player), iconId)) return
        val delivered = chat.sendIcon(friend, player, iconId)
        if (delivered == null) {
            refuse(session, friendId, InstantMessageRefusal.UNDELIVERED, "")
            return
        }
        chat.show(session.client, messages.chat(delivered, friendId, senderId, trackingId))
        IconAchievements.used(player, AchievementTally.running())
    }

    private fun chatPartner(
        session: PacketSession,
        friendId: Int,
        text: String,
    ): Friend? {
        val player = session.player
        val friend = player.messenger.friend(friendId)
        val receiver = players.find(friendId)
        val mayTalk = player.stats.standing.mayTalk()
        if (mayTalk && !player.stats.messengerPace.take(System.currentTimeMillis())) return null
        if (mayTalk && friend == null && players.profileOf(friendId) == null) {
            session.client.send(ErrorReport.forRequest(InstantMessageRefusal.SEND_REQUEST))
            return null
        }
        val reason =
            when {
                !mayTalk -> InstantMessageRefusal.SENDER_MUTED
                friend == null -> InstantMessageRefusal.NOT_A_FRIEND
                messenger.blocks.blocks(friendId, player.profile.id) -> InstantMessageRefusal.RECIPIENT_UNAVAILABLE
                friend.shownOnline && receiver != null && !receiver.stats.standing.mayTalk() ->
                    InstantMessageRefusal.RECEIVER_MUTED
                else -> null
            }
        if (reason == null) return friend
        refuse(session, friendId, reason, text)
        return null
    }

    private fun refuse(
        session: PacketSession,
        friendId: Int,
        reason: Int,
        text: String,
    ) = session.send(InstantMessageErrorOutCodec, InstantMessageErrorOut(reason, friendId, text))

    private fun history(
        session: PacketSession,
        chatId: Int,
        before: String,
    ) {
        if (!historyCooldown.acquire(session.client)) return
        val userId = session.player.profile.id
        session.send(
            ConsoleMessageHistoryOutCodec,
            messages.history(chatId, messenger.history.page(userId, chatId, before)),
        )
    }

    private fun inviteFriends(
        session: PacketSession,
        userIds: List<Int>,
        rawMessage: String,
    ) {
        val player = session.player
        if (!player.inRoom || !player.stats.standing.mayTalk()) return
        val typed = FriendInput.inviteText(rawMessage)
        val invited =
            FriendInput
                .batch(userIds, PlayerLimits.maxFriends(player))
                .mapNotNull { player.messenger.friend(it) }
        if (typed.isEmpty() || invited.isEmpty()) return
        if (!inviteCooldown.acquire(session.client)) return
        val message = FriendInput.inviteText(wordFilter.filter(typed, player))

        // A friend who appears offline gets no invite, so the answer tells nothing about them.
        val reached =
            invited
                .filter { it.shownOnline }
                .mapNotNull { players.find(it.id) }
                .filterNot { it.stats.settings.noRoomInvites || it.stats.contacts.userBlocked(player.profile.id) }
        reached.forEach { it.connection.send(RoomInviteOutCodec, RoomInviteOut(player.profile.id, message)) }
        if (reached.isNotEmpty()) PlayerActions.progress(player, CountedAction.SEND_MESSENGER_INVITE, 1)
        if (reached.size < invited.size) {
            session.send(
                RoomInviteErrorOutCodec,
                RoomInviteErrorOut(ERROR_RECIPIENT_UNAVAILABLE, RoomInviteErrorOut.Branch1.Else),
            )
        }
    }

    private fun findNewFriends(session: PacketSession) {
        if (!findCooldown.acquire(session.client)) return
        val player = session.player
        val allowed = rooms.categoriesForRank(player.profile.rank.id).mapTo(HashSet()) { it.id }
        val here = live().roomOf(player.profile.id)
        val candidates = rooms.popular(Int.MAX_VALUE).filter { it.category in allowed && it.id != here }
        val room = MeetingRoom.choose(candidates) { ThreadLocalRandom.current().nextInt(it) }
        if (room == null) {
            session.send(FindFriendsProcessResultOutCodec, FindFriendsProcessResultOut(false))
        } else {
            session.client.send(RoomForwardOutCodec, RoomForwardOut(room.id))
        }
    }

    private fun follow(
        session: PacketSession,
        friendId: Int,
    ) {
        if (followCooldown.acquire(session.client)) followFriend(session, friendId)
    }

    private fun followFriend(
        session: PacketSession,
        friendId: Int,
    ) {
        val client = session.client
        val player = session.player
        val entry = if (friendId > 0) player.messenger.friend(friendId) else null
        if (entry == null) {
            session.followFailed(NOT_IN_FRIEND_LIST)
            return
        }
        // A friend who appears offline answers exactly like one who is.
        val friend =
            players.find(friendId)?.takeIf { entry.shownOnline && !it.stats.contacts.userBlocked(player.profile.id) }
        if (friend == null) {
            session.followFailed(FRIEND_OFFLINE)
            return
        }
        // A friend who disallows following answers as on the landing view, also from the same room.
        val port = live()
        val friendRoom = port.roomOf(friendId)
        if ((friend.stats.settings.noFollowing && !player.hasPermission(Privilege.FRIENDS_FOLLOW_ANY)) ||
            friendRoom == null
        ) {
            session.followFailed(FRIEND_NOT_IN_ROOM)
            return
        }
        client.send(RoomForwardOutCodec, RoomForwardOut(friendRoom))
        if (friendRoom != port.roomOf(player.profile.id)) {
            PlayerActions.progress(player, CountedAction.FOLLOW_FRIEND, 1)
        }
    }

    private fun visitUser(
        session: PacketSession,
        rawName: String,
    ) {
        if (!followCooldown.acquire(session.client)) return
        val username = FriendInput.name(rawName)
        val friend = username?.let { friendByName(session.player.messenger.friends.values, it) }
        if (friend != null) {
            followFriend(session, friend.id)
        } else {
            val exists = username != null && players.readProfile(username) != null
            session.followFailed(if (exists) NOT_IN_FRIEND_LIST else FRIEND_OFFLINE)
        }
    }

    private fun PacketSession.followFailed(code: Int) = send(FollowFriendFailedOutCodec, FollowFriendFailedOut(code))

    companion object {
        private val LOGGER = LoggerFactory.getLogger(FriendsPackets::class.java)

        private const val FRIEND_LIST_OWN_FULL = 1
        private const val FRIEND_LIST_TARGET_FULL = 2
        private const val TARGET_NOT_ACCEPTING_REQUESTS = 3
        private const val TARGET_NOT_FOUND = 4
        private const val NO_SUCH_REQUEST = 5
        private const val TARGET_BLOCKED_BY_ASKER = 8
        private const val ERROR_RECIPIENT_UNAVAILABLE = 0

        private const val NOT_IN_FRIEND_LIST = 0
        private const val FRIEND_OFFLINE = 1
        private const val FRIEND_NOT_IN_ROOM = 2

        fun friendByName(
            friends: Iterable<Friend>,
            name: String,
        ): Friend? = friends.firstOrNull { name.equals(it.username, ignoreCase = true) }
    }
}
