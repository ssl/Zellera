package zel.wired.trigger

import zel.room.RoomScope
import zel.room.event.RoomEvent
import zel.room.unit.UnitKind
import zel.wired.box.BoxSettings
import zel.wired.box.TriggerBox
import zel.wired.firing.Firing

class EnterRoom(
    override val furniId: Int,
    override val settings: BoxSettings,
    private val playerName: (RoomScope, Int) -> String?,
) : TriggerBox {
    override val needsUser = true

    override fun matches(
        event: RoomEvent,
        firing: Firing,
    ): Boolean = event is RoomEvent.Entered && (settings.text.isEmpty() || named(event.unitIndex, firing))

    private fun named(
        unitIndex: Int,
        firing: Firing,
    ): Boolean {
        val scope = firing.scope
        val kind = scope.room.units[unitIndex]?.kind as? UnitKind.Player ?: return false
        return playerName(scope, kind.playerId).equals(settings.text, ignoreCase = true)
    }
}
