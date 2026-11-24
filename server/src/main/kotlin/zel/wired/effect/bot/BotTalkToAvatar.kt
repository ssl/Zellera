package zel.wired.effect.bot

import zel.room.RoomScope
import zel.room.unit.UnitKind
import zel.room.unit.bot.RoomBots
import zel.wired.box.BoxSettings
import zel.wired.box.EffectBox
import zel.wired.firing.Firing
import zel.wired.firing.Targets

class BotTalkToAvatar(
    override val furniId: Int,
    override val settings: BoxSettings,
    private val bots: RoomBots,
    private val talks: BotTalks,
    private val nameOf: (RoomScope, Int) -> String?,
) : EffectBox {
    override val negative = false
    override val needsUser = false

    override fun run(firing: Firing) {
        val line = BotChoice.rest(settings.text)
        if (line.isEmpty()) return
        val whisper = settings.ints.getOrNull(BotTalk.MODE) == WHISPER
        val width = settings.ints.getOrNull(BotTalk.WIDTH) ?: -1
        val hide = BotTalk.tail(width, (settings.ints.getOrNull(BotTalk.HIDE_NAME) ?: 0) != 0)
        val users = Targets.units(firing, this, USER_SLOT).filter { it.kind is UnitKind.Player }
        val talkers = BotChoice.bots(firing, this, bots)
        users.forEach { user ->
            talkers.forEach { (bot, _) ->
                talks.later(firing, bot.bot.id) { scope ->
                    val text = talks.texts.fill(firing, user, line)
                    if (whisper) {
                        bots.whisper(scope, bot, user, text, width, hide)
                    } else {
                        val name = (user.kind as? UnitKind.Player)?.let { nameOf(scope, it.playerId) }.orEmpty()
                        bots.say(scope, bot, "$name: $text", shout = false, width = width, hideName = hide)
                    }
                }
            }
        }
    }

    private companion object {
        const val WHISPER = 1
        const val USER_SLOT = 1
    }
}
