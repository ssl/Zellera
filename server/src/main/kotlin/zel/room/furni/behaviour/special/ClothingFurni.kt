package zel.room.furni.behaviour.special

import zel.protocol.generated.StuffData
import zel.room.RoomScope
import zel.room.furni.RoomFurni
import zel.room.furni.Toggleable
import zel.room.furni.Usable
import zel.room.furni.place.StuffOnWire
import kotlin.random.Random

class ClothingFurni :
    Usable,
    Toggleable,
    StuffOnWire {
    override fun onUse(
        scope: RoomScope,
        furni: RoomFurni,
        playerId: Int,
        param: Int,
    ) = Unit

    override fun toggle(
        scope: RoomScope,
        furni: RoomFurni,
        backward: Boolean,
    ): Boolean = false

    override fun toggleRandom(
        scope: RoomScope,
        furni: RoomFurni,
        random: Random,
    ): Boolean = false

    override fun stuff(furni: RoomFurni): StuffData = SpecialStuff.strings(furni, listOf(""))
}
