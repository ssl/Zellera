package zel.protocol.outgoing.inventory

import zel.game.players.inventory.AvatarEffect
import zel.protocol.generated.PlayerEffectAddedOut
import zel.protocol.generated.PlayerEffectAddedOutCodec
import zel.protocol.outgoing.OutgoingMessage

class PlayerEffectAdded(
    val effect: AvatarEffect,
) : OutgoingMessage<PlayerEffectAddedOut>(PlayerEffectAddedOutCodec) {
    override fun message() =
        PlayerEffectAddedOut(
            effectType = effect.id,
            subType = 0,
            duration = if (effect.duration > 0) effect.duration else Int.MAX_VALUE,
            permanent = effect.duration <= 0,
        )
}
