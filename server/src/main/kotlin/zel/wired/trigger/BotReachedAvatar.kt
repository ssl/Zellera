package zel.wired.trigger

import zel.room.RoomScope
import zel.room.event.RoomEvent
import zel.wired.box.BoxSettings
import zel.wired.box.TriggerBox
import zel.wired.firing.Firing

class BotReachedAvatar(
    override val furniId: Int,
    override val settings: BoxSettings,
    botName: (RoomScope, Int) -> String?,
) : TriggerBox {
    private val bot = BotListener(settings, botName)

    override fun matches(
        event: RoomEvent,
        firing: Firing,
    ): Boolean = event is RoomEvent.BotReachedUnit && bot.listens(event.botIndex, firing.scope)

    override fun answers(firing: Firing): Boolean =
        bot.answers((firing.event as? RoomEvent.BotReachedUnit)?.botIndex, firing)
}
