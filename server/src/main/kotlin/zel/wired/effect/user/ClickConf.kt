package zel.wired.effect.user

import zel.protocol.generated.WiredClickSettingsOut
import zel.protocol.generated.WiredClickSettingsOutCodec
import zel.room.tick.Audience
import zel.room.tick.Lane
import zel.wired.box.BoxSettings
import zel.wired.box.EffectBox
import zel.wired.firing.Firing

class ClickConf(
    override val furniId: Int,
    override val settings: BoxSettings,
) : EffectBox {
    override val negative = false
    override val needsUser = false

    override fun run(firing: Firing) {
        val message = WiredClickSettingsOut(settings.ints.getOrNull(0) ?: 0, settings.ints.getOrNull(1) ?: 0)
        UserTargets.players(firing, this).forEach { (_, playerId) ->
            firing.scope.out.send(Lane.Now, Audience.Player(playerId), WiredClickSettingsOutCodec, message)
        }
    }
}
