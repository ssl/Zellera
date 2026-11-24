package zel.protocol.outgoing.icons

import zel.game.icons.IconRoster
import zel.protocol.generated.PlayerIconsOut
import zel.protocol.generated.PlayerIconsOutCodec
import zel.protocol.outgoing.OutgoingMessage

class PlayerIcons(
    private val snapshot: IconRoster,
) : OutgoingMessage<PlayerIconsOut>(PlayerIconsOutCodec) {
    override fun message() = playerIcons(snapshot)
}
