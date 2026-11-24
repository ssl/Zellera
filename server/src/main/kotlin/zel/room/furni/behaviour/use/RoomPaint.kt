package zel.room.furni.behaviour.use

import zel.game.achievements.GameEvents
import zel.protocol.generated.FurniListRemoveOut
import zel.protocol.generated.FurniListRemoveOutCodec
import zel.protocol.generated.RoomPropertyOut
import zel.protocol.generated.RoomPropertyOutCodec
import zel.room.RoomScope
import zel.room.entry.DoorCheck
import zel.room.furni.place.FurniInventory
import zel.room.furni.place.FurniOut
import zel.room.furni.place.InventoryItem
import zel.room.furni.place.PlaceError
import zel.room.tick.Audience
import zel.room.tick.Lane

class RoomPaint(
    private val inventory: FurniInventory,
    private val store: PaperStore,
    private val progress: (playerId: Int, event: String) -> Unit,
) {
    interface PaperStore {
        fun paint(
            roomId: Int,
            kind: String,
            value: String,
            rowId: Int,
            ownerId: Int,
        )
    }

    fun paint(
        scope: RoomScope,
        playerId: Int,
        level: Int,
        itemId: Int,
    ) {
        val item = inventory.find(playerId, itemId)
        when {
            item == null -> removed(scope, playerId, itemId)
            level < DoorCheck.OWNER -> FurniOut.error(scope, playerId, PlaceError.NotOwner)
            else -> spend(scope, playerId, item)
        }
    }

    private fun spend(
        scope: RoomScope,
        playerId: Int,
        item: InventoryItem,
    ) {
        val kind = KINDS.firstOrNull { item.definition.name.contains(it) } ?: return
        if (!inventory.take(playerId, item.id)) return
        progress(playerId, GameEvents.of(GameEvents.ROOM_PAINTED, kind))
        val property = RoomPropertyOut(kind, item.state)
        scope.out.send(Lane.Now, Audience.Room, RoomPropertyOutCodec, property)
        removed(scope, playerId, item.id)
        store.paint(scope.room.info.id, kind, item.state, item.id, playerId)
    }

    private fun removed(
        scope: RoomScope,
        playerId: Int,
        itemId: Int,
    ) = scope.out.send(Lane.Now, Audience.Player(playerId), FurniListRemoveOutCodec, FurniListRemoveOut(itemId))

    private companion object {
        /** The room property each paper sets; also the detail of the painted event. */
        val KINDS = listOf("wallpaper", "floor", "landscape")
    }
}
