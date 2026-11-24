package zel.protocol.outgoing.users

import zel.game.players.inventory.BadgeHolders
import zel.game.players.inventory.BadgeTier
import zel.protocol.generated.BadgeInfoOut
import zel.protocol.generated.BadgeInfoOutCodec
import zel.protocol.outgoing.OutgoingMessage

class BadgeInfo(
    val badgeId: Int,
    val badgeCode: String,
    private val ownerCounts: BadgeHolders,
) : OutgoingMessage<BadgeInfoOut>(BadgeInfoOutCodec) {
    override fun message(): BadgeInfoOut {
        val ownerCount = ownerCounts.count(badgeCode)
        return BadgeInfoOut(badgeId, badgeCode, ownerCount, BadgeTier.forHolders(ownerCount).code)
    }
}
