package zel.wired.variable

import zel.room.Room
import zel.room.furni.RoomFurni
import zel.room.unit.RoomUnit
import zel.room.unit.UnitKind

sealed interface Holder {
    data class Player(
        val id: Int,
    ) : Holder

    data class Bot(
        val id: Int,
    ) : Holder

    data class Pet(
        val id: Int,
    ) : Holder

    data class Furni(
        val id: Int,
    ) : Holder

    data object Room : Holder

    data object Context : Holder

    companion object {
        fun of(unit: RoomUnit): Holder =
            when (val kind = unit.kind) {
                is UnitKind.Player -> Player(kind.playerId)
                is UnitKind.Bot -> Bot(kind.botId)
                is UnitKind.Pet -> Pet(kind.petId)
            }

        fun of(furni: RoomFurni): Holder = Furni(furni.id)
    }
}

/** The unit this holder is in [room]; none for furni, the room or a context. */
fun Holder.unitIn(room: Room): RoomUnit? =
    room.units
        .all()
        .firstOrNull { Holder.of(it) == this }

/** The furni's id while it is in [room], else the unit's room index; none for the room or a context. */
fun Holder.entityIn(room: Room): Int? =
    when (this) {
        is Holder.Furni -> id.takeIf { room.furni[it] != null }
        Holder.Room, Holder.Context -> null
        else -> unitIn(room)?.index
    }
