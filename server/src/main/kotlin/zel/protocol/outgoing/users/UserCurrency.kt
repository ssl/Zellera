package zel.protocol.outgoing.users

import zel.game.economy.EconomyLedger
import zel.game.players.Player
import zel.protocol.generated.ActivityPointsOut
import zel.protocol.generated.ActivityPointsOutCodec
import zel.protocol.outgoing.OutgoingMessage

class UserCurrency(
    val player: Player,
) : OutgoingMessage<ActivityPointsOut>(ActivityPointsOutCodec) {
    override fun message(): ActivityPointsOut =
        ActivityPointsOut(
            EconomyLedger.pointTypes().map { type -> ActivityPointsOut.List1Item(type, player.profile.balance(type)) },
        )
}
