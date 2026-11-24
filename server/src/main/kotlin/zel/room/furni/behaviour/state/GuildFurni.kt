package zel.room.furni.behaviour.state

import zel.protocol.generated.StuffData
import zel.room.furni.RoomFurni
import zel.room.furni.behaviour.special.SpecialStuff
import zel.room.furni.place.StuffOnWire

class GuildFurni(
    part: StatePart,
    private val guildFields: (Int) -> List<String>?,
) : Multistate(part),
    StuffOnWire {
    override fun stuff(furni: RoomFurni): StuffData = stuffOf(furni, guildFields)

    companion object {
        fun stuffOf(
            furni: RoomFurni,
            guildFields: (Int) -> List<String>?,
        ): StuffData {
            val fields = guildFields(furni.groupId) ?: return SpecialStuff.legacy(furni, furni.state)
            return SpecialStuff.strings(furni, listOf(furni.state) + fields)
        }
    }
}
