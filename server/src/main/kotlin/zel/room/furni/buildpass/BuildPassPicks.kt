package zel.room.furni.buildpass

import zel.room.RoomScope
import zel.room.furni.RoomFurni

interface BuildPassPicks {
    /** Read before the room's lock; null for any furni that is not Build Pass. */
    fun before(furniId: Int): BuildPassPickup?

    fun counts(
        scope: RoomScope,
        before: BuildPassPickup,
    )

    fun gone(
        scope: RoomScope,
        furni: RoomFurni,
        before: BuildPassPickup?,
    )

    fun swept(
        scope: RoomScope,
        befores: List<BuildPassPickup>,
    )

    companion object {
        val None: BuildPassPicks =
            object : BuildPassPicks {
                override fun before(furniId: Int): BuildPassPickup? = null

                override fun counts(
                    scope: RoomScope,
                    before: BuildPassPickup,
                ) = Unit

                override fun gone(
                    scope: RoomScope,
                    furni: RoomFurni,
                    before: BuildPassPickup?,
                ) = Unit

                override fun swept(
                    scope: RoomScope,
                    befores: List<BuildPassPickup>,
                ) = Unit
            }
    }
}
