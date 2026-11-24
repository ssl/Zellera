package zel.wired.trigger

import zel.protocol.generated.WiredClickPlayerResponseOut
import zel.protocol.generated.WiredClickPlayerResponseOutCodec
import zel.room.RoomScope
import zel.room.event.RoomEvent
import zel.room.map.Point
import zel.room.tick.Audience
import zel.room.tick.Lane
import zel.room.unit.RoomUnit
import zel.room.unit.UnitKind
import zel.wired.BoxSettingsSource
import zel.wired.box.BoxSettings

class ClickUsers(
    private val turn: (clicker: RoomUnit, to: Point) -> Unit = { _, _ -> },
    private val forms: BoxSettingsSource,
) {
    fun clicked(
        scope: RoomScope,
        clicker: RoomUnit,
        index: Int,
    ) {
        val playerId = (clicker.kind as? UnitKind.Player)?.playerId ?: return
        // An index with no unit gets no answer at all.
        val clicked = scope.room.units[index] ?: return
        // Bots and pets fire too; conditions decide. The menu stays shut only when a block-menu stack held for this
        // click.
        val event = RoomEvent.ClickUser(clicker.index, index)
        scope.raise(event)
        val response = WiredClickPlayerResponseOut(index, !event.menu.blocked)
        scope.out.send(Lane.Now, Audience.Player(playerId), WiredClickPlayerResponseOutCodec, response)
        val forms = clickForms(scope)
        if (forms.isNotEmpty() && forms.all(::turns)) turn(clicker, clicked.tile)
    }

    private fun clickForms(scope: RoomScope): List<BoxSettings?> =
        scope.room.furni
            .withInteraction(ClickUser.CODE)
            .map { forms.settings(scope.room, it) }

    /** The clicker turns to the clicked unit only while no click-user box has "do not turn" (int 2) on. */
    private fun turns(form: BoxSettings?): Boolean = (form?.ints?.getOrNull(1) ?: 0) != ClickUser.DOES_NOT_TURN
}
