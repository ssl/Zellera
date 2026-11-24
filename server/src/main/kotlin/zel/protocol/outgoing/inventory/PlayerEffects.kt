package zel.protocol.outgoing.inventory

import zel.game.players.Player
import zel.game.players.inventory.AvatarEffect
import zel.protocol.generated.PlayerEffectsOut
import zel.protocol.generated.PlayerEffectsOutCodec
import zel.protocol.outgoing.OutgoingMessage

/** Only a running temporary effect counts as active. */
class PlayerEffects(
    private val player: Player,
    private val effects: Collection<AvatarEffect>,
) : OutgoingMessage<PlayerEffectsOut>(PlayerEffectsOutCodec) {
    override fun message() = PlayerEffectsOut(entries(effects, player.services.unixTimestamp()))

    companion object {
        private const val NO_SUBTYPE = 0

        fun entries(
            effects: Collection<AvatarEffect>,
            now: Int,
        ): List<PlayerEffectsOut.EffectsItem> =
            effects.map { effect ->
                PlayerEffectsOut.EffectsItem(
                    effectType = effect.id,
                    subType = NO_SUBTYPE,
                    duration = if (effect.isPermanent) Int.MAX_VALUE else effect.duration,
                    inactiveCount = effect.idleCopies(),
                    secondsLeft = effect.secondsRunning(now),
                    permanent = effect.isPermanent,
                )
            }
    }
}
