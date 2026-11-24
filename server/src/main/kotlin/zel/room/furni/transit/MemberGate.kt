package zel.room.furni.transit

import zel.protocol.generated.StuffData
import zel.room.RoomScope
import zel.room.furni.RoomFurni
import zel.room.furni.Steppable
import zel.room.furni.behaviour.state.GuildFurni
import zel.room.furni.place.FurniAccess
import zel.room.furni.place.StuffOnWire
import zel.room.unit.RoomUnit
import zel.room.unit.walk.StepGate

class MemberGate(
    private val rooms: TransitRooms,
    private val gates: Gates,
    val rule: GateRule,
    private val guildFields: (Int) -> List<String>? = { null },
) : StepGate,
    Steppable,
    TransitUse,
    StuffOnWire {
    override fun stuff(furni: RoomFurni): StuffData = GuildFurni.stuffOf(furni, guildFields)

    override fun lets(
        scope: RoomScope,
        furni: RoomFurni,
        unit: RoomUnit,
    ): Boolean = gates.lets(furni, unit, rule)

    override fun refused(
        scope: RoomScope,
        furni: RoomFurni,
        unit: RoomUnit,
    ) = gates.refused(scope, unit, rule)

    override fun onStepOn(
        scope: RoomScope,
        furni: RoomFurni,
        unit: RoomUnit,
    ) = gates.stepOn(scope, rooms.of(scope.room), furni, rule)

    override fun onStepOff(
        scope: RoomScope,
        furni: RoomFurni,
        unit: RoomUnit,
    ) = gates.stepOff(scope, rooms.of(scope.room), furni, unit, rule)

    override fun use(
        scope: RoomScope,
        furni: RoomFurni,
        unit: RoomUnit,
        access: FurniAccess,
    ) {
        if (!gates.lets(furni, unit, rule)) gates.refused(scope, unit, rule)
    }
}
