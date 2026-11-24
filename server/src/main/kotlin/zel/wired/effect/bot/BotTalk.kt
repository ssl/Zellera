package zel.wired.effect.bot

import zel.room.unit.bot.RoomBots
import zel.wired.box.BoxSettings
import zel.wired.box.EffectBox
import zel.wired.firing.Firing

/** wf_act_bot_talk: ints [0 talk or 1 shout, width, hide name]. */
class BotTalk(
    override val furniId: Int,
    override val settings: BoxSettings,
    private val bots: RoomBots,
    private val talks: BotTalks,
) : EffectBox {
    override val negative = false
    override val needsUser = false

    override fun run(firing: Firing) {
        val line = BotChoice.rest(settings.text)
        if (line.isEmpty()) return
        val shout = settings.ints.getOrNull(MODE) == SHOUT
        val width = settings.ints.getOrNull(WIDTH) ?: -1
        val hide = (settings.ints.getOrNull(HIDE_NAME) ?: 0) != 0
        BotChoice.bots(firing, this, bots).forEach { (bot, unit) ->
            talks.later(firing, bot.bot.id) { scope ->
                // A bot line fills %roomname% too; the other % words stay as written.
                val text = talks.texts.fill(firing, unit, line).replace(ROOM_NAME, scope.room.info.name)
                bots.say(scope, bot, text, shout, width, tail(width, hide))
            }
        }
    }

    companion object {
        const val MODE = 0
        const val WIDTH = 1
        const val HIDE_NAME = 2
        const val SHOUT = 1
        private const val ROOM_NAME = "%roomname%"

        /** The hide flag goes on the wire only when the box sets a width or hides the name. */
        fun tail(
            width: Int,
            hide: Boolean,
        ): Boolean? = if (width == -1 && !hide) null else hide
    }
}
