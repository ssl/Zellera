package zel.room.life

import zel.room.RoomScope
import zel.room.unit.RoomUnit

interface LifeHooks {
    fun leave(
        scope: RoomScope,
        unit: RoomUnit,
    ) {}

    fun restoreLook(
        scope: RoomScope,
        unit: RoomUnit,
    ) {}

    fun handItemReceived(
        playerId: Int,
        item: Int,
    ) {}

    fun dancing(unit: RoomUnit): Boolean = false

    fun someoneWaits(scope: RoomScope): Boolean = false

    fun entering(roomId: Int): Boolean = false

    /** True for a navigator category whose rooms are public. Any thread. */
    fun publicCategory(categoryId: Int): Boolean = false

    fun decoHosting(
        ownerId: Int,
        amount: Int,
    ) {}

    companion object {
        val None: LifeHooks = object : LifeHooks {}
    }
}
