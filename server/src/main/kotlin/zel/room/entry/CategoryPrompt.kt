package zel.room.entry

import zel.core.rules.rules
import zel.protocol.generated.ShowEnforceRoomCategoryDialogOut
import zel.protocol.generated.ShowEnforceRoomCategoryDialogOutCodec
import zel.room.RoomInfo
import zel.room.RoomScope
import zel.room.tick.Audience
import zel.room.tick.Lane

class CategoryPrompt(
    private val needsCategory: (RoomInfo) -> Boolean,
    private val visitorsNeeded: () -> Int = { rules.roomEnforceCategoryVisitors },
) {
    fun afterEntry(
        scope: RoomScope,
        door: RoomDoor,
    ) {
        val needed = visitorsNeeded()
        if (needed <= 0 || door.categoryAsked || door.visitors() < needed) return
        val info = scope.room.info
        if (info.ownerId !in door.present || !needsCategory(info)) return
        door.categoryAsked = true
        scope.out.send(
            Lane.Now,
            Audience.Player(info.ownerId),
            ShowEnforceRoomCategoryDialogOutCodec,
            ShowEnforceRoomCategoryDialogOut(SELECTION_TYPE),
        )
    }

    private companion object {
        /** The dialog is the same for every selection type. */
        const val SELECTION_TYPE = 0
    }
}
