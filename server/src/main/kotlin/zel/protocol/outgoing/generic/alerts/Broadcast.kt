package zel.protocol.outgoing.generic.alerts

import zel.game.players.Player
import zel.protocol.generated.BroadcastOut
import zel.protocol.generated.BroadcastOutCodec
import zel.protocol.outgoing.OutgoingMessage

class Broadcast(
    private val text: String,
) : OutgoingMessage<BroadcastOut>(BroadcastOutCodec) {
    constructor(text: String, player: Player) : this(text.replace("%username%", player.profile.name))

    override fun message() = BroadcastOut(text)
}
