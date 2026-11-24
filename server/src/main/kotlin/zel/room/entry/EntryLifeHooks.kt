package zel.room.entry

import zel.room.RoomScope
import zel.room.life.LifeHooks
import zel.room.unit.RoomUnit

class EntryLifeHooks(
    private val leaving: RoomLeaving,
    private val next: LifeHooks = LifeHooks.None,
) : LifeHooks by next {
    override fun leave(
        scope: RoomScope,
        unit: RoomUnit,
    ) = leaving.leave(scope, unit)

    override fun someoneWaits(scope: RoomScope): Boolean = leaving.doorOf(scope.room).ringing.isNotEmpty()
}
