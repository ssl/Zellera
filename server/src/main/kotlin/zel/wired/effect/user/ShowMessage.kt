package zel.wired.effect.user

import zel.room.chat.ChatKind
import zel.room.unit.RoomUnit
import zel.room.unit.bot.UnitSpeech
import zel.wired.box.BoxSettings
import zel.wired.box.EffectBox
import zel.wired.firing.Firing

class ShowMessage(
    override val furniId: Int,
    override val settings: BoxSettings,
    private val host: UserEffectHost,
    private val texts: TextPlaceholders,
) : EffectBox {
    override val negative = false
    override val needsUser = false

    override fun run(firing: Firing) {
        if (settings.text.isEmpty()) return
        val style = settings.ints.getOrNull(STYLE) ?: 0
        val width = settings.ints.getOrNull(WIDTH) ?: -1
        if ((settings.ints.getOrNull(VISIBILITY) ?: 0) != 0) {
            toRoom(firing, style, width)
        } else {
            // Filled when the box runs, only the sending waits for the flash (in a says firing whose line holds, for
            // that line): the receivers of a signal this stack sends run before that and may change what it prints.
            val lines = UserTargets.players(firing, this).map { (unit, _) -> unit to filled(firing, unit) }
            firing.flash.afterLine { scope ->
                lines.forEach { (unit, text) ->
                    if (scope.room.units[unit.index] === unit) {
                        UnitSpeech.whisper(scope, unit, unit, text, style, width)
                        host.wake(scope, unit)
                    }
                }
            }
        }
    }

    private fun toRoom(
        firing: Firing,
        style: Int,
        width: Int,
    ) {
        val speaker = firing.triggeringUnit?.let { firing.scope.room.units[it] } ?: return
        val text = filled(firing, speaker)
        firing.flash.afterLine { scope ->
            UnitSpeech.toRoom(scope, speaker, ChatKind.Talk, text, style, width = width)
        }
    }

    /**
     * The filled text without its trailing spaces: a box saved as 'You pay $(x)... ' prints
     * 'You pay 10...', and one ending in a line break keeps it. Leading spaces stay.
     */
    private fun filled(
        firing: Firing,
        unit: RoomUnit,
    ) = texts.fill(firing, unit, settings.text).trimEnd(' ')

    companion object {
        const val VISIBILITY = 0
        const val STYLE = 1
        const val WIDTH = 2
    }
}
