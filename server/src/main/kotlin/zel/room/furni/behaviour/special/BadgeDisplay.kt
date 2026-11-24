package zel.room.furni.behaviour.special

import zel.protocol.generated.StuffData
import zel.room.RoomScope
import zel.room.furni.RoomFurni
import zel.room.furni.Toggleable
import zel.room.furni.Usable
import zel.room.furni.place.StuffOnWire
import kotlin.random.Random

class BadgeDisplay :
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

    override fun stuff(furni: RoomFurni): StuffData = SpecialStuff.strings(furni, listOf("0") + strings(furni.state))

    companion object {
        private const val ENGRAVED_FIELDS = 3

        fun strings(state: String): List<String> {
            val fields = state.split('\t')
            if (fields.size == ENGRAVED_FIELDS) return listOf(fields[2], fields[0], fields[1])
            val code = if (state.isEmpty() || state == "0") "" else state
            return listOf(code, "", "")
        }
    }
}
