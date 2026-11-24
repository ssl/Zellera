package zel.wired.effect.user

import zel.room.furni.behaviour.state.StateHooks
import zel.room.unit.bot.UnitSpeech
import zel.wired.box.BoxSettings
import zel.wired.box.EffectBox
import zel.wired.firing.Firing

class MuteTriggerer(
    override val furniId: Int,
    override val settings: BoxSettings,
    private val hooks: StateHooks,
    private val host: UserEffectHost,
) : EffectBox {
    override val negative = false
    override val needsUser = false

    override fun run(firing: Firing) {
        val scope = firing.scope
        val minutes = (settings.ints.getOrNull(0) ?: FRESH_MINUTES).coerceIn(MIN_MINUTES, MAX_MINUTES)
        UserTargets.players(firing, this).forEach { (unit, playerId) ->
            if (!hooks.hasRights(scope, playerId)) {
                host.mute(scope, playerId, minutes)
                if (settings.text.isNotEmpty()) UnitSpeech.whisper(scope, unit, unit, settings.text, WHISPER_STYLE)
            }
        }
    }

    companion object {
        const val FRESH_MINUTES = 5
        const val MIN_MINUTES = 1
        const val MAX_MINUTES = 1440
        const val WHISPER_STYLE = 34
    }
}
