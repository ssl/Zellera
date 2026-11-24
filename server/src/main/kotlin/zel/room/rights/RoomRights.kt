package zel.room.rights

import zel.protocol.generated.FlatControllerAddedOut
import zel.protocol.generated.FlatControllerAddedOutCodec
import zel.protocol.generated.FlatControllerRemovedOut
import zel.protocol.generated.FlatControllerRemovedOutCodec
import zel.protocol.generated.FlatControllersOut
import zel.protocol.generated.FlatControllersOutCodec
import zel.protocol.generated.OutgoingCodec
import zel.protocol.generated.RaidProtectionCapabilityOut
import zel.protocol.generated.RaidProtectionCapabilityOutCodec
import zel.protocol.generated.WiredPermissionsOut
import zel.protocol.generated.WiredPermissionsOutCodec
import zel.protocol.generated.YouAreControllerOut
import zel.protocol.generated.YouAreControllerOutCodec
import zel.protocol.generated.YouAreNotControllerOut
import zel.protocol.generated.YouAreNotControllerOutCodec
import zel.room.RoomRegistry
import zel.room.RoomScope
import zel.room.entry.DoorCheck
import zel.room.entry.EntryViews
import zel.room.entry.RoomLeaving
import zel.room.tick.Audience
import zel.room.tick.Lane
import zel.room.unit.UnitStatus

class RoomRights(
    private val registry: RoomRegistry,
    private val leaving: RoomLeaving,
    private val store: RightsStore,
    private val persistInOrder: (key: Any, job: Runnable) -> Unit,
    private val views: EntryViews = EntryViews.None,
    private val ejectFurni: (RoomScope, Int) -> Unit = { _, _ -> },
) {
    fun levelOf(playerId: Int): Pair<Int, Int>? {
        val roomId = registry.roomOf(playerId) ?: return null
        return registry.withRoom(roomId) { scope -> roomId to leaving.doorOf(scope.room).levelOf(playerId) }
    }

    fun give(
        giverId: Int,
        targetId: Int,
    ) {
        val roomId = registry.roomOf(giverId) ?: return
        if (!isOwnerIn(roomId, giverId) || store.hasRights(roomId, targetId)) return
        val storedName = store.playerName(targetId)
        registry.withRoom(roomId) { scope ->
            val door = leaving.doorOf(scope.room)
            val target = door.present[targetId]
            val name = target?.card?.name ?: storedName ?: return@withRoom
            if (door.levelOf(giverId) < DoorCheck.OWNER || (target?.level ?: 0) >= DoorCheck.OWNER) return@withRoom
            persistInOrder(Key(roomId)) { store.addRights(roomId, targetId) }
            if (target != null) changeLevel(scope, targetId, maxOf(target.level, DoorCheck.RIGHTS))
            send(scope, giverId, FlatControllerAddedOutCodec, FlatControllerAddedOut(roomId, targetId, name))
        }
    }

    fun take(
        ownerId: Int,
        targetIds: List<Int>,
    ) {
        val roomId = registry.roomOf(ownerId) ?: return
        if (!isOwnerIn(roomId, ownerId)) return
        val holders = store.rightsHolders(roomId).map { it.id }.toSet()
        registry.withRoom(roomId) { scope ->
            if (leaving.doorOf(scope.room).levelOf(ownerId) < DoorCheck.OWNER) return@withRoom
            targetIds.distinct().filter { it in holders }.forEach { removed(scope, ownerId, it) }
        }
    }

    fun takeAll(
        ownerId: Int,
        roomId: Int,
        isOwner: Boolean,
        reply: Reply,
    ) {
        if (!isOwner) return
        val holders = store.rightsHolders(roomId).map { it.id }
        val handled =
            registry.withRoom(roomId) { scope ->
                holders.forEach { removed(scope, ownerId, it) }
            }
        if (handled == null) {
            holders.forEach {
                store.removeRights(roomId, it)
                reply.send(FlatControllerRemovedOutCodec, FlatControllerRemovedOut(roomId, it))
            }
        }
    }

    fun giveUp(
        playerId: Int,
        roomId: Int,
    ) {
        if (registry.roomOf(playerId) != roomId || !store.hasRights(roomId, playerId)) return
        registry.withRoom(roomId) { scope -> removed(scope, scope.room.info.ownerId, playerId) }
    }

    fun list(
        roomId: Int,
        isOwner: Boolean,
        reply: Reply,
    ) {
        if (!isOwner) return
        val holders = store.rightsHolders(roomId).map { FlatControllersOut.List1Item(it.id, it.name) }
        reply.send(FlatControllersOutCodec, FlatControllersOut(roomId, holders))
    }

    fun changeLevel(
        scope: RoomScope,
        playerId: Int,
        level: Int,
    ) {
        val door = leaving.doorOf(scope.room)
        val present = door.present[playerId] ?: return
        if (present.level == level) return
        door.present[playerId] = present.copy(level = level)
        val roomId = scope.room.info.id
        scope.room.units[present.unitIndex]?.let { unit ->
            if (level >= DoorCheck.RIGHTS) {
                unit.setStatus(UnitStatus.FLAT_CONTROL, level.toString())
            } else {
                unit.dropStatus(UnitStatus.FLAT_CONTROL)
            }
        }
        if (level >= DoorCheck.RIGHTS) {
            send(scope, playerId, YouAreControllerOutCodec, YouAreControllerOut(roomId, level))
        } else {
            send(scope, playerId, YouAreNotControllerOutCodec, YouAreNotControllerOut(roomId))
        }
        val (modify, read) = views.wiredPermissions(scope, level)
        send(scope, playerId, WiredPermissionsOutCodec, WiredPermissionsOut(modify, read))
        send(
            scope,
            playerId,
            RaidProtectionCapabilityOutCodec,
            RaidProtectionCapabilityOut(roomId, level >= DoorCheck.RIGHTS),
        )
    }

    /** From the room in memory, so a request from anyone but an owner never reaches the database. */
    private fun isOwnerIn(
        roomId: Int,
        playerId: Int,
    ): Boolean =
        registry.withRoom(roomId) { scope ->
            leaving.doorOf(scope.room).levelOf(playerId) >= DoorCheck.OWNER
        } == true

    private fun removed(
        scope: RoomScope,
        toId: Int,
        targetId: Int,
    ) {
        val roomId = scope.room.info.id
        persistInOrder(Key(roomId)) { store.removeRights(roomId, targetId) }
        leaving.doorOf(scope.room).present[targetId]?.let { present ->
            if (present.level == DoorCheck.RIGHTS) changeLevel(scope, targetId, 0)
        }
        send(scope, toId, FlatControllerRemovedOutCodec, FlatControllerRemovedOut(roomId, targetId))
        ejectFurni(scope, targetId)
    }

    private fun <T> send(
        scope: RoomScope,
        playerId: Int,
        codec: OutgoingCodec<T>,
        message: T,
    ) = scope.out.send(Lane.Now, Audience.Player(playerId), codec, message)

    /** One write lane per room, so a grant and a later removal reach the database in that order. */
    private data class Key(
        val roomId: Int,
    )
}
