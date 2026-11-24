package zel.room.furni.place

import zel.db.DeadlockRetry
import zel.protocol.generated.HeightMapUpdateOutCodec
import zel.protocol.generated.ItemAddOutCodec
import zel.protocol.generated.ItemRemoveOutCodec
import zel.protocol.generated.ObjectAddOutCodec
import zel.protocol.generated.ObjectRemoveConfirmOut
import zel.protocol.generated.ObjectRemoveConfirmOutCodec
import zel.protocol.generated.ObjectRemoveOutCodec
import zel.room.RoomScope
import zel.room.RoomWrites
import zel.room.furni.HeightMapChange
import zel.room.furni.RoomFurni
import zel.room.furni.buildpass.BuildPassPicks
import zel.room.furni.buildpass.BuildPassPickup
import zel.room.persist.FurniHandBack
import zel.room.persist.FurniTransfers
import zel.room.tick.Audience
import zel.room.tick.Lane
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.concurrent.CompletableFuture

/** The row and the owner's inventory are written after the room answered, on [writes]. */
class FurniPickup(
    private val inventory: FurniInventory,
    private val transfers: FurniTransfers,
    private val writes: RoomWrites,
    private val units: UnitsOnTiles = UnitsOnTiles.None,
    private val confirm: PickupConfirm = PickupConfirm.Never,
    private val buildPass: BuildPassPicks = BuildPassPicks.None,
    private val inRoom: (Int, (RoomScope) -> Unit) -> Unit = { _, _ -> },
    private val left: (RoomScope, RoomFurni) -> Unit = { _, _ -> },
) {
    fun before(furniId: Int): BuildPassPickup? = buildPass.before(furniId)

    fun pickup(
        scope: RoomScope,
        access: FurniAccess,
        category: Int,
        furniId: Int,
        confirmed: Boolean,
        before: BuildPassPickup? = null,
    ) {
        val room = scope.room
        val furni = room.furni[furniId] ?: return
        if (category != categoryOf(furni) || Interactions.of(furni.definition) == Interactions.POST_IT) return
        when (access.pickup(furni.placerId, room.info.ownerId, furni.isBuildPass)) {
            PickupRight.Refused -> return FurniOut.error(scope, access.playerId, PlaceError.NotOwner)
            PickupRight.Allowed -> Unit
        }
        if (!confirmed && confirm.needed(scope, furni)) {
            scope.out.send(
                Lane.Now,
                Audience.Player(access.playerId),
                ObjectRemoveConfirmOutCodec,
                ObjectRemoveConfirmOut(category, furni.id, CONFIRM_TITLE, CONFIRM_BODY),
            )
            return
        }
        remove(scope, furni, access, before)
    }

    fun takeOut(
        scope: RoomScope,
        furni: RoomFurni,
        picker: Int = furni.ownerId,
    ): Boolean = take(scope, furni, picker, null)

    /** [toOwner] returns the row to its owner. */
    fun unplace(
        roomId: Int,
        furni: RoomFurni,
        toOwner: Boolean,
    ) = inRoom(roomId) { scope ->
        if (scope.room.furni[furni.id] === furni && take(scope, furni, furni.ownerId, null) && toOwner) {
            inventory.handBack(furni.ownerId, furni.id)
        }
    }

    fun sweep(
        scope: RoomScope,
        pickerId: Int,
        quietly: Boolean = false,
        byId: Boolean = false,
        matching: (RoomFurni) -> Boolean,
    ): CompletableFuture<Boolean> {
        val room = scope.room
        val ordered = if (byId) room.furni.all().sortedBy { it.id } else room.furni.inSweepOrder()
        val gone =
            ordered
                .filter(matching)
                .filterNot { confirm.staysOnSweep(scope, it) }
        val built = gone.filter { it.isBuildPass }.map { it.id }
        if (built.isNotEmpty()) countSwept(room.info.id, built)
        gone.forEach { take(scope, it, pickerId, null, sweeping = true) }
        val back = gone.filterNot { it.isBuildPass }
        val written = CompletableFuture<Boolean>()
        if (back.isEmpty()) {
            written.complete(true)
        } else {
            val states = back.map { FurniHandBack(it.id, it.state) }
            writes.write(room.info.id, Runnable { written.complete(handBackAll(room.info.id, back, states, quietly)) })
        }
        return written
    }

    fun ejectFurniOf(
        scope: RoomScope,
        playerId: Int,
    ) {
        if (scope.room.furni
                .all()
                .none { it.ownerId == playerId }
        ) {
            return
        }
        inventory.refresh(playerId)
        sweep(scope, playerId, quietly = true, byId = true) { it.ownerId == playerId }
    }

    /** Reads on the write lane before the deletes; answers on the room thread. */
    private fun countSwept(
        roomId: Int,
        furniIds: List<Int>,
    ) = writes.write(
        roomId,
        Runnable {
            val befores = furniIds.mapNotNull(::before)
            if (befores.isNotEmpty()) inRoom(roomId) { scope -> buildPass.swept(scope, befores) }
        },
    )

    @Suppress("TooGenericExceptionCaught") // the furni goes back on any failure
    private fun handBackAll(
        roomId: Int,
        furni: List<RoomFurni>,
        states: List<FurniHandBack>,
        quietly: Boolean,
    ): Boolean {
        val moved =
            try {
                DeadlockRetry.run { transfers.takeAllToInventory(roomId, states) }.toSet()
            } catch (e: Exception) {
                log.error("room {}: {} furni failed to go back to their owners", roomId, furni.size, e)
                putBack(roomId, furni)
                return false
            }
        furni.filter { it.id in moved }.groupBy { it.ownerId }.forEach { (owner, handed) ->
            val ids = handed.map { it.id }
            if (quietly) inventory.handBackQuietly(owner, ids) else inventory.handBackAll(owner, ids)
        }
        return true
    }

    private fun putBack(
        roomId: Int,
        furni: List<RoomFurni>,
    ) = inRoom(roomId) { scope ->
        furni.filter { scope.room.furni[it.id] == null }.forEach { standAgain(scope, it) }
    }

    private fun standAgain(
        scope: RoomScope,
        furni: RoomFurni,
    ) {
        val room = scope.room
        val tiles = furni.tiles()
        val heights = HeightMapChange.of(room, tiles)
        (furni.behaviour as? Placeable)?.beforeAdded(furni)
        room.furni.add(furni)
        if (furni.isWall) {
            scope.out.send(Lane.Now, Audience.Room, ItemAddOutCodec, FurniWire.itemAdd(furni))
        } else {
            scope.out.send(Lane.Now, Audience.Room, ObjectAddOutCodec, FurniWire.objectAdd(furni))
            FurniOut.heights(scope, heights)
        }
        (furni.behaviour as? Placeable)?.onPlaced(scope, furni)
        units.furniChanged(scope, emptyList(), tiles)
    }

    private fun remove(
        scope: RoomScope,
        furni: RoomFurni,
        access: FurniAccess,
        before: BuildPassPickup?,
    ) {
        val room = scope.room
        val picker = if (access.isRoomOwner && access.playerId != room.info.ownerId) access.playerId else furni.ownerId
        if (take(scope, furni, picker, before)) {
            if (!furni.isBuildPass) handBack(room.info.id, furni)
        }
    }

    private fun take(
        scope: RoomScope,
        furni: RoomFurni,
        picker: Int,
        before: BuildPassPickup?,
        sweeping: Boolean = false,
    ): Boolean {
        val room = scope.room
        val tiles = furni.tiles()
        val heights = HeightMapChange.of(room, tiles)
        val released = (furni.behaviour as? Placeable)?.released(scope, furni).orEmpty()
        room.furni.remove(furni.id) ?: return false
        left(scope, furni)
        if (furni.isBuildPass && before != null) buildPass.counts(scope, before)
        if (furni.isWall) {
            scope.out.send(Lane.Now, Audience.Room, ItemRemoveOutCodec, FurniWire.itemRemove(furni, picker))
        } else {
            val remove = FurniWire.objectRemove(furni, picker)
            scope.out.send(Lane.Now, Audience.Room, ObjectRemoveOutCodec, remove)
            (furni.behaviour as? Placeable)?.onLifted(scope, furni)
            if (sweeping) {
                (furni.behaviour as? Placeable)?.onPickedUp(scope, furni)
                heights.with(released).allTiles().forEach {
                    scope.out.send(Lane.Now, Audience.Room, HeightMapUpdateOutCodec, it)
                }
                (furni.behaviour as? Placeable)?.onTilesFreed(scope, furni)
                units.furniChanged(scope, tiles, emptyList())
                if (furni.isBuildPass) buildPass.gone(scope, furni, before)
                return true
            }
            FurniOut.heights(scope, heights.with(released))
            (furni.behaviour as? Placeable)?.onTilesFreed(scope, furni)
        }
        (furni.behaviour as? Placeable)?.onPickedUp(scope, furni)
        units.furniChanged(scope, tiles, emptyList())
        if (furni.isBuildPass) buildPass.gone(scope, furni, before)
        return true
    }

    @Suppress("TooGenericExceptionCaught") // the furni goes back on any failure
    private fun handBack(
        roomId: Int,
        furni: RoomFurni,
    ) {
        val id = furni.id
        val owner = furni.ownerId
        val state = furni.state
        writes.write(
            roomId,
            Runnable {
                try {
                    val moved = DeadlockRetry.run { transfers.takeToInventory(roomId, id, state) }
                    if (moved) {
                        inventory.handBack(owner, id)
                    } else {
                        log.warn("room {}: picked-up furni {} was no longer in the room's rows", roomId, id)
                    }
                } catch (e: Exception) {
                    log.error("room {}: furni {} failed to go back to its owner", roomId, id, e)
                    putBack(roomId, listOf(furni))
                }
            },
        )
    }

    private companion object {
        val log: Logger = LoggerFactory.getLogger(FurniPickup::class.java)

        const val FLOOR = 2
        const val WALL = 1
        const val CONFIRM_TITLE = "wiredfurni.variables.pickup.title"
        const val CONFIRM_BODY = "wiredfurni.variables.pickup.body"

        fun categoryOf(furni: RoomFurni): Int = if (furni.isWall) WALL else FLOOR
    }
}
