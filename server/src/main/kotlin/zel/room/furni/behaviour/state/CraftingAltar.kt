package zel.room.furni.behaviour.state

import zel.room.RoomScope
import zel.room.furni.RoomFurni

/**
 * A crafting table: the client opens its crafting window on a double-click, and the use it sends with it leaves the
 * furni's state as it is. A wired toggle still steps it like any furni.
 */
class CraftingAltar(
    part: StatePart,
) : Multistate(part) {
    override fun clicked(
        scope: RoomScope,
        furni: RoomFurni,
    ): String? = null
}
