package zel.room.unit.pet

import zel.room.RoomListener
import zel.room.RoomScope
import zel.room.event.RoomEvent
import zel.room.unit.UnitKind

class PetListener(
    private val life: PetLife,
    private val commands: PetCommands,
    private val care: PetCare,
) : RoomListener {
    override fun on(
        scope: RoomScope,
        event: RoomEvent,
    ) {
        when (event) {
            is RoomEvent.Says -> {
                val speaker = scope.room.units[event.unitIndex] ?: return
                if (speaker.kind is UnitKind.Player) commands.heard(scope, speaker, event.text)
            }

            is RoomEvent.Entered -> {
                scope.room.units[event.unitIndex]?.let { life.ownerEntered(scope, it) }
            }

            is RoomEvent.Left -> {
                care.riderLeft(scope, event.unitIndex)
            }

            is RoomEvent.StepOn -> {
                life.steppedOn(scope, event.unitIndex, event.furniId)
            }

            is RoomEvent.StepOff -> {
                life.steppedOff(scope, event.unitIndex, event.furniId)
            }

            else -> {}
        }
    }
}
