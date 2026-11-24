package zel.protocol.outgoing.inventory

import zel.game.players.Player
import zel.game.players.inventory.BadgeTier
import zel.protocol.generated.BadgesOut
import zel.protocol.generated.BadgesOutCodec
import zel.protocol.outgoing.OutgoingMessage

class InventoryBadges(
    val player: Player?,
) : OutgoingMessage<BadgesOut>(BadgesOutCodec) {
    override fun message(): BadgesOut? {
        val owner = player ?: return null
        val badges =
            owner.inventory.badges.all.map {
                val ownerCount =
                    owner.services.badgeCaches.ownerCounts
                        .count(it.code)
                BadgesOut.BadgesItem(it.id, it.code, ownerCount, BadgeTier.forHolders(ownerCount).code)
            }
        return BadgesOut(totalFragments = 1, fragmentNo = 0, badges = badges)
    }
}
