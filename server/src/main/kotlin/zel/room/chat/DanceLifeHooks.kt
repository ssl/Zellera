package zel.room.chat

import zel.room.life.LifeHooks
import zel.room.unit.RoomUnit

class DanceLifeHooks(
    private val dances: Dances,
    private val next: LifeHooks = LifeHooks.None,
) : LifeHooks by next {
    override fun dancing(unit: RoomUnit): Boolean = dances.dancing(unit) || next.dancing(unit)
}
