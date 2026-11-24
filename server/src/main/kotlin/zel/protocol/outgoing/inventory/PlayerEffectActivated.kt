package zel.protocol.outgoing.inventory

import zel.game.players.inventory.AvatarEffect
import zel.protocol.generated.PlayerEffectActivatedOut
import zel.protocol.generated.PlayerEffectActivatedOutCodec
import zel.protocol.outgoing.OutgoingMessage

class PlayerEffectActivated(
    val effect: AvatarEffect,
) : OutgoingMessage<PlayerEffectActivatedOut>(PlayerEffectActivatedOutCodec) {
    override fun message() =
        PlayerEffectActivatedOut(
            effectType = effect.id,
            duration = if (effect.isPermanent) Int.MAX_VALUE else effect.duration,
            permanent = effect.isPermanent,
        )
}
