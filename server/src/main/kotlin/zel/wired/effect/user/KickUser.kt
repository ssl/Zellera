package zel.wired.effect.user

import zel.game.effects.EffectRoles
import zel.protocol.generated.PlayerEffectOut
import zel.protocol.generated.PlayerEffectOutCodec
import zel.protocol.generated.YouArePlayingGameOut
import zel.protocol.generated.YouArePlayingGameOutCodec
import zel.room.RoomScope
import zel.room.tick.Audience
import zel.room.tick.Lane
import zel.room.unit.RoomUnit
import zel.room.unit.bot.UnitSpeech
import zel.wired.box.BoxSettings
import zel.wired.box.EffectBox
import zel.wired.firing.Firing
import zel.wired.timing.RoomPoster

class KickUser(
    override val furniId: Int,
    override val settings: BoxSettings,
    private val state: UserEffectState,
    private val host: UserEffectHost,
    private val poster: RoomPoster,
    private val texts: TextPlaceholders,
) : EffectBox {
    override val negative = false
    override val needsUser = false

    override fun run(firing: Firing) {
        val scope = firing.scope
        UserTargets.players(firing, this).forEach { (unit, playerId) ->
            val exception = host.kickException(scope, playerId)
            if (exception != null) {
                UnitSpeech.whisper(scope, unit, unit, exception, EXCEPTION_STYLE)
            } else {
                whisper(firing, unit)
                kick(scope, unit, playerId)
            }
        }
    }

    private fun whisper(
        firing: Firing,
        unit: RoomUnit,
    ) {
        if (settings.text.isEmpty()) return
        val text = texts.fill(firing, unit, settings.text)
        UnitSpeech.whisper(firing.scope, unit, unit, text, WHISPER_STYLE)
    }

    private fun kick(
        scope: RoomScope,
        unit: RoomUnit,
        playerId: Int,
    ) {
        scope.out.send(
            Lane.Now,
            Audience.Room,
            PlayerEffectOutCodec,
            PlayerEffectOut(unit.index, EffectRoles.of(EffectRoles.TELEPORT), 0),
        )
        if (!state.of(scope.room).kicking.add(unit.index)) return
        val room = scope.room
        val posted =
            poster.post(room.info.id, KICK_DELAY_MILLIS) { later ->
                if (later.room !== room) return@post
                state.of(room).kicking.remove(unit.index)
                if (later.room.units[unit.index] !== unit) return@post
                later.out.send(
                    Lane.Now,
                    Audience.Player(playerId),
                    YouArePlayingGameOutCodec,
                    YouArePlayingGameOut(false),
                )
                host.kickOut(later, unit)
            }
        if (!posted) state.of(room).kicking.remove(unit.index)
    }

    companion object {
        const val EXCEPTION_STYLE = 34
        const val WHISPER_STYLE = 34
        const val KICK_DELAY_MILLIS = 2000L
    }
}
