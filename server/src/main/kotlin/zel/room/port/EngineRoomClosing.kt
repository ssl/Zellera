package zel.room.port

import zel.game.players.Player
import zel.room.OrderedRoomWrites
import zel.room.RoomScope
import zel.room.furni.FurniSaves
import zel.room.furni.place.FurniPickup
import zel.room.protocol.RoomSidePackets
import zel.room.unit.bot.RoomBots
import zel.room.unit.pet.RoomPets
import java.util.concurrent.CompletableFuture

/** Closing a room on the engine: a delete sweeps its furni, bots and pets back to the owner first. */
class EngineRoomClosing(
    private val pickups: FurniPickup,
    private val bots: RoomBots,
    private val pets: RoomPets,
    private val saves: FurniSaves,
    private val writes: OrderedRoomWrites,
    private val store: GroupRoomStore,
    private val side: RoomSidePackets,
    private val player: (Int) -> Player?,
    private val persisted: () -> Boolean,
) : RoomClosing {
    override fun empty(
        scope: RoomScope,
        owner: Player,
    ): CompletableFuture<Boolean> {
        val furni = pickups.sweep(scope, owner.profile.id) { true }
        bots.all(scope).forEach { (bot, _) -> bots.pickUp(scope, owner, bot.bot.id) }
        pets.all(scope).forEach { pets.pickUp(scope, owner, it.pet.id) }
        return furni
    }

    override fun written() = persisted()

    override fun ejectFurni(
        scope: RoomScope,
        ownerId: Int,
    ) = pickups.ejectFurniOf(scope, ownerId)

    override fun saveFurni(scope: RoomScope) {
        saves.saveDirty(scope.room)
        writes.flush(scope.room.info.id)
    }

    override fun settle(roomId: Int) {
        writes.settle(roomId)
    }

    override fun storeModeration(
        roomId: Int,
        newName: String?,
        lock: Boolean,
    ) = store.moderate(roomId, newName, lock)

    override fun storeNoGroup(roomId: Int) = store.clearGroup(roomId)

    override fun sendInfo(
        playerId: Int,
        roomId: Int,
    ) {
        player(playerId)?.let { side.sendInfo(it, roomId, enterRoom = false, forward = true) }
    }
}
