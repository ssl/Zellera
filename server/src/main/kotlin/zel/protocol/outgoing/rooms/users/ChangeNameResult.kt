package zel.protocol.outgoing.rooms.users

import zel.game.players.Player
import zel.protocol.generated.ChangePlayerNameResultOut
import zel.protocol.generated.ChangePlayerNameResultOutCodec
import zel.protocol.outgoing.OutgoingMessage

class ChangeNameResult(
    private val player: Player,
) : OutgoingMessage<ChangePlayerNameResultOut>(ChangePlayerNameResultOutCodec) {
    override fun message() = ChangePlayerNameResultOut(0, player.profile.name, emptyList())
}
