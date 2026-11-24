package zel.wired.effect.bot

import zel.room.unit.RoomUnit
import zel.room.unit.UnitKind
import zel.room.unit.bot.RoomBot
import zel.room.unit.bot.RoomBots
import zel.wired.box.BoxSettings
import zel.wired.box.WiredBox
import zel.wired.firing.Firing
import zel.wired.firing.Targets
import zel.wired.firing.UserSource

/** Which bots a bot box acts on: every bot of the name before the text's tab, or those of its source. */
object BotChoice {
    fun bots(
        firing: Firing,
        box: WiredBox,
        bots: RoomBots,
    ): List<Pair<RoomBot, RoomUnit>> {
        val source = box.settings.userSources.getOrNull(0) ?: UserSource.BY_NAME
        val found =
            if (source == UserSource.BY_NAME) {
                bots.named(firing.scope, name(box.settings.text))
            } else {
                Targets
                    .units(
                        firing,
                        box,
                        0,
                    ).filter { it.kind is UnitKind.Bot }
                    .mapNotNull { bots.byIndex(firing.scope, it.index) }
            }
        return found.mapNotNull { bot ->
            firing.scope.room.units[bot.unitIndex]
                ?.let { bot to it }
        }
    }

    /** Bots chosen by name with no name given. */
    fun unnamed(settings: BoxSettings): Boolean {
        val source = settings.userSources.getOrNull(0) ?: UserSource.BY_NAME
        return source == UserSource.BY_NAME && name(settings.text).isBlank()
    }

    fun name(text: String): String = text.substringBefore(TAB)

    fun rest(text: String): String = text.substringAfter(TAB, "")

    private const val TAB = '\t'
}
