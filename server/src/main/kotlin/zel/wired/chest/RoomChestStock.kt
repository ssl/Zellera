package zel.wired.chest

import zel.room.RoomScope
import zel.room.furni.RoomFurni
import zel.wired.condition.ChestStock

class RoomChestStock(
    private val chests: RoomChests,
) : ChestStock {
    override fun total(
        scope: RoomScope,
        chest: RoomFurni,
    ): Long? = chests.chest(chest)?.takeIf { it.wiredEnabled }?.let { it.credits.toLong() + it.furni.size }

    override fun count(
        scope: RoomScope,
        chest: RoomFurni,
        types: Set<Int>,
    ): Long? =
        chests.chest(chest)?.takeIf { it.wiredEnabled }?.let { held ->
            held.furni.count { it.definitionId in types }.toLong()
        }
}
