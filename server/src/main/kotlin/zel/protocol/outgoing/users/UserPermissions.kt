package zel.protocol.outgoing.users

import zel.game.permissions.SecurityLevel
import zel.game.players.Player
import zel.protocol.generated.PlayerRightsOut
import zel.protocol.generated.PlayerRightsOutCodec
import zel.protocol.outgoing.OutgoingMessage

class UserPermissions(
    val player: Player,
) : OutgoingMessage<PlayerRightsOut>(PlayerRightsOutCodec) {
    val clubLevel: Int = if (player.stats.club.isMember) 2 else 0

    override fun message(): PlayerRightsOut {
        val level = SecurityLevel.of(player)
        return PlayerRightsOut(clubLevel, level, SecurityLevel.sendsAmbassador(level))
    }
}
