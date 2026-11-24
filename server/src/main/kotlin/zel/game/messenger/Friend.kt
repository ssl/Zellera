package zel.game.messenger

import zel.game.players.Gender
import zel.game.players.Player

/** One friend list entry: a connected friend's live state counts, else the last told; hidden online shows offline. */
@Suppress("LongParameterList") // one friend list row
class Friend(
    val id: Int,
    val ownerId: Int,
    username: String,
    look: String,
    private val connected: ConnectedPlayers,
    relation: Short = 0,
    gender: Gender = Gender.M,
    motto: String = "",
    val categoryId: Int = 0,
    lastKnownOnline: Boolean = false,
    lastKnownInRoom: Boolean = false,
) {
    @Volatile var username: String = username
        private set

    @Volatile var look: String = look
        private set

    @Volatile var gender: Gender = gender
        private set

    @Volatile var motto: String = motto
        private set

    /** The owner's relation to this friend: 0 none, 1 heart, 2 smile, 3 skull. */
    @Volatile var relation: Short = relation

    @Volatile private var lastKnownOnline: Boolean = lastKnownOnline

    @Volatile private var lastKnownInRoom: Boolean = lastKnownInRoom

    private val live: Player? get() = connected.byId(id)

    val isOnline: Boolean get() = live?.profile?.isOnline ?: lastKnownOnline

    val shownOnline: Boolean
        get() = live?.let { it.profile.isOnline && !it.stats.settings.appearOffline } ?: lastKnownOnline

    val shownInRoom: Boolean
        get() = live?.let { it.profile.isOnline && !it.stats.settings.appearOffline && it.inRoom } ?: lastKnownInRoom

    /** False for a friend who is away: nobody is followed to nowhere. */
    val allowsFollowing: Boolean get() = live?.let { !it.stats.settings.noFollowing } ?: false

    fun learn(
        friend: Player,
        visibleOnline: Boolean,
        visibleInRoom: Boolean,
    ) {
        val profile = friend.profile
        username = profile.name
        look = profile.figure
        gender = profile.gender
        motto = profile.motto
        lastKnownOnline = visibleOnline
        lastKnownInRoom = visibleInRoom
    }
}
