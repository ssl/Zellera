package zel.room.games

import zel.room.Room
import zel.room.RoomScope
import zel.room.furni.RoomFurni
import zel.room.unit.RoomUnit
import zel.room.unit.path.ClosedToWalks
import zel.room.unit.walk.StepGate

class TeamScoreboard(
    val family: FieldFamily,
    val color: TeamColor,
) : FieldFurni(),
    StepGate,
    ClosedToWalks {
    override fun closed(
        room: Room,
        furni: RoomFurni,
    ): Boolean = true

    override fun lets(
        scope: RoomScope,
        furni: RoomFurni,
        unit: RoomUnit,
    ): Boolean = false

    override fun refused(
        scope: RoomScope,
        furni: RoomFurni,
        unit: RoomUnit,
    ) = Unit
}
