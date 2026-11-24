package zel.protocol.outgoing.inventory

import zel.game.players.inventory.AvatarEffect
import zel.protocol.generated.PlayerEffectExpiredOut
import zel.protocol.generated.PlayerEffectExpiredOutCodec
import zel.protocol.outgoing.OutgoingMessage

class PlayerEffectExpired(
    val effect: AvatarEffect,
) : OutgoingMessage<PlayerEffectExpiredOut>(PlayerEffectExpiredOutCodec) {
    override fun message() = PlayerEffectExpiredOut(effect.id)
}
