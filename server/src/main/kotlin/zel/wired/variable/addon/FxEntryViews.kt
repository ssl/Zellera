package zel.wired.variable.addon

import zel.room.RoomScope
import zel.room.entry.EntryViews
import zel.room.unit.RoomUnit

/** Sends a newcomer the room's variable FX after its entry burst. */
class FxEntryViews(
    private val fx: VariableFx,
    private val next: EntryViews,
) : EntryViews by next {
    override fun afterBurst(
        scope: RoomScope,
        unit: RoomUnit,
        playerId: Int,
    ) {
        next.afterBurst(scope, unit, playerId)
        fx.entered(scope, unit, playerId)
    }
}
