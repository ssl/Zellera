package zel.wired.trigger

import zel.room.RoomScope
import zel.room.event.RoomEvent
import zel.wired.box.BoxSettings
import zel.wired.firing.Firing

class BotReachedFurni(
    override val furniId: Int,
    override val settings: BoxSettings,
    botName: (RoomScope, Int) -> String?,
) : FurniTrigger {
    private val bot = BotListener(settings, botName)

    override fun matches(
        event: RoomEvent,
        firing: Firing,
    ): Boolean =
        event is RoomEvent.BotReachedFurni &&
            listens(event.furniId) &&
            bot.listens(event.botIndex, firing.scope) &&
            onTop(event, firing.scope)

    override fun answers(firing: Firing): Boolean = super.answers(firing) && bot.answers(firing.triggeringUnit, firing)

    private fun onTop(
        event: RoomEvent.BotReachedFurni,
        scope: RoomScope,
    ): Boolean {
        val tile = scope.room.units[event.botIndex]?.tile ?: return false
        return scope.room.furni
            .at(tile)
            .topItem()
            ?.id == event.furniId
    }
}
