package zel.protocol.outgoing.users

import zel.game.players.Player
import zel.protocol.generated.FigureUpdateOut
import zel.protocol.generated.FigureUpdateOutCodec
import zel.protocol.outgoing.OutgoingMessage

class UpdateUserLook(
    val player: Player,
) : OutgoingMessage<FigureUpdateOut>(FigureUpdateOutCodec) {
    override fun message() = FigureUpdateOut(player.profile.figure, player.profile.gender.name)
}
