package zel.protocol.outgoing.users

import zel.game.players.PlayerBadge
import zel.game.players.inventory.BadgeHolders
import zel.game.players.inventory.BadgeTier
import zel.protocol.generated.BadgeReceivedOut
import zel.protocol.generated.BadgeReceivedOutCodec
import zel.protocol.outgoing.OutgoingMessage

class AddUserBadge(
    val badge: PlayerBadge,
    private val ownerCounts: BadgeHolders,
) : OutgoingMessage<BadgeReceivedOut>(BadgeReceivedOutCodec) {
    override fun message(): BadgeReceivedOut {
        val ownerCount = ownerCounts.count(badge.code)
        return BadgeReceivedOut(badge.id, badge.code, ownerCount, BadgeTier.forHolders(ownerCount).code)
    }
}
