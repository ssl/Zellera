package zel.wired.condition

import zel.room.RoomScope
import zel.room.furni.RoomFurni

interface ChestStock {
    fun total(
        scope: RoomScope,
        chest: RoomFurni,
    ): Long?

    fun count(
        scope: RoomScope,
        chest: RoomFurni,
        types: Set<Int>,
    ): Long?

    companion object {
        val NONE: ChestStock =
            object : ChestStock {
                override fun total(
                    scope: RoomScope,
                    chest: RoomFurni,
                ): Long? = null

                override fun count(
                    scope: RoomScope,
                    chest: RoomFurni,
                    types: Set<Int>,
                ): Long? = null
            }
    }
}
