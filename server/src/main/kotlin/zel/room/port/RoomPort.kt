package zel.room.port

import zel.game.players.Player
import zel.protocol.Outbound
import zel.protocol.generated.OutgoingCodec
import zel.room.RoomDirectory

@Suppress("TooManyFunctions") // one per question the rest of the server asks a room
interface RoomPort :
    RoomDirectory,
    RoomCommands {
    fun userCount(roomId: Int): Int = playersIn(roomId).size

    fun groupOf(roomId: Int): Int

    /** The navigator category of the loaded room; 0 when it is not loaded. */
    fun categoryOf(roomId: Int): Int

    fun summary(roomId: Int): RoomSummary?

    fun assignGroup(
        roomId: Int,
        groupId: Int,
    )

    fun send(
        roomId: Int,
        message: Outbound<*>,
    )

    fun <T> send(
        roomId: Int,
        codec: OutgoingCodec<T>,
        message: T,
    ) = send(roomId, Outbound(codec, message))

    fun unitIndex(playerId: Int): Int?

    fun furniIn(
        roomId: Int,
        furniId: Int,
    ): PlacedFurni?

    fun furniClasses(roomId: Int): Set<String> = emptySet()

    fun nameChanged(playerId: Int)

    fun groupRightsChanged(playerId: Int)

    fun groupLookChanged(
        roomId: Int,
        groupId: Int,
    ) = Unit

    fun syncBuildPassRooms(ownerId: Int): Int = 0

    fun leftGame(playerId: Int)

    fun leaveForGame(playerId: Int): Int

    /** Puts a staff question to a loaded room; false when none runs there or one already does. */
    fun askRoomQuestion(
        roomId: Int,
        question: String,
        millis: Long,
    ): Boolean

    fun answerRoomQuestion(
        playerId: Int,
        questionId: Int,
        answer: String,
    )

    fun respectShown(
        fromId: Int,
        toId: Int,
        total: Int,
    )

    fun showIcon(
        playerId: Int,
        iconId: Int,
        ignoresMutes: Boolean,
    ): Boolean

    fun stopTrade(playerId: Int)

    fun appearanceChanged(playerId: Int)

    /** The room's card of [playerId] read again (favourite group, account level); nothing is sent. */
    fun cardChanged(playerId: Int) = Unit

    fun kick(playerId: Int)

    fun kickAll(
        roomId: Int,
        spared: (playerId: Int) -> Boolean,
    ) = playersIn(roomId).filterNot(spared).forEach(::kick)

    fun refreshRights(playerId: Int)

    fun mayDelete(roomId: Int): Boolean

    fun emptyForDelete(
        roomId: Int,
        owner: Player,
    ): Boolean

    fun close(roomId: Int)

    fun moderate(
        roomId: Int,
        newName: String?,
        lock: Boolean,
    )

    fun ownerRenamed(
        ownerId: Int,
        newName: String,
    )

    fun groupRemoved(roomId: Int)

    fun sendRoomInfo(
        playerId: Int,
        roomId: Int,
    )

    fun ejectFurniOf(
        roomId: Int,
        ownerId: Int,
    )

    /** Waits until the state is in the database, so a copy of its rows reads it. */
    fun saveFurni(roomId: Int)
}
