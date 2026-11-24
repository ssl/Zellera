package zel.room.port

import zel.game.players.Player
import zel.room.RoomScope
import java.util.concurrent.CompletableFuture

interface RoomClosing {
    fun empty(
        scope: RoomScope,
        owner: Player,
    ): CompletableFuture<Boolean>

    fun written(): Boolean

    fun ejectFurni(
        scope: RoomScope,
        ownerId: Int,
    )

    fun saveFurni(scope: RoomScope)

    fun settle(roomId: Int)

    fun storeModeration(
        roomId: Int,
        newName: String?,
        lock: Boolean,
    )

    fun storeNoGroup(roomId: Int)

    fun sendInfo(
        playerId: Int,
        roomId: Int,
    )

    object None : RoomClosing {
        override fun empty(
            scope: RoomScope,
            owner: Player,
        ): CompletableFuture<Boolean> = CompletableFuture.completedFuture(true)

        override fun written() = true

        override fun ejectFurni(
            scope: RoomScope,
            ownerId: Int,
        ) = Unit

        override fun saveFurni(scope: RoomScope) = Unit

        override fun settle(roomId: Int) = Unit

        override fun storeModeration(
            roomId: Int,
            newName: String?,
            lock: Boolean,
        ) = Unit

        override fun storeNoGroup(roomId: Int) = Unit

        override fun sendInfo(
            playerId: Int,
            roomId: Int,
        ) = Unit
    }
}
