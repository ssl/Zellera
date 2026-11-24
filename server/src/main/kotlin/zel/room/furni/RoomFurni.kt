package zel.room.furni

import zel.game.items.FurniHeights
import zel.game.items.Item
import zel.game.items.ProductKind
import zel.room.map.Footprint
import zel.room.map.Point

/** Owned by its room's lock; [dirty] marks a change the store has not written yet. */
class RoomFurni(
    val id: Int,
    val ownerId: Int,
    val ownerName: String,
    val definition: Item,
    val behaviour: FurniBehaviour,
    anchor: Point?,
    var z: Double,
    direction: Int,
    var state: String,
    var wallPosition: String?,
    val groupId: Int,
    val limitedData: String,
    val rowId: Int = id,
    val placerId: Int = ownerId,
    val placerName: String = ownerName,
    /** Made by a Place furni box: never spent, redeemed or opened. */
    val wiredCopy: Boolean = false,
) {
    var anchor: Point? = anchor
        private set

    var direction: Int = direction
        private set

    var dirty: Boolean = false
        set(value) {
            field = value
            standsIn?.marked(this, value)
        }

    internal var standsIn: RoomFurniSet? = null

    val kind: FurniKind = FurniKind.of(definition)

    val isWall: Boolean get() = definition.type == ProductKind.WALL_FURNI

    val isBuildPass: Boolean get() = BuildPass.isClientId(id)

    val isSpendable: Boolean get() = !isBuildPass && !wiredCopy

    val isStackable: Boolean get() = definition.canStack

    val height: Double get() = FurniHeight.current(definition, kind, state)

    val top: Double get() = FurniHeights.stack(z, height)

    val heightOnWire: String get() = FurniHeight.onWire(definition, kind, state)

    fun tiles(): List<Point> = Footprint.of(this)

    internal fun relocate(
        newAnchor: Point,
        newDirection: Int,
    ) {
        anchor = newAnchor
        direction = newDirection
    }
}
