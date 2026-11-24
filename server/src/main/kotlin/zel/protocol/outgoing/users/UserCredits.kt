package zel.protocol.outgoing.users

import zel.game.players.Player
import zel.protocol.generated.CreditBalanceOut
import zel.protocol.generated.CreditBalanceOutCodec
import zel.protocol.outgoing.OutgoingMessage

class UserCredits private constructor(
    val player: Player?,
    private val fixedCredits: Int,
) : OutgoingMessage<CreditBalanceOut>(CreditBalanceOutCodec) {
    constructor(player: Player) : this(player, -1)

    constructor(credits: Int) : this(null, maxOf(0, credits))

    val credits: Int get() = player?.profile?.credits ?: fixedCredits

    override fun message() = CreditBalanceOut("$credits.0")
}
