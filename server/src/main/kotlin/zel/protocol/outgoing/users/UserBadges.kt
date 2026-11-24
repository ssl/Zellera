package zel.protocol.outgoing.users

import zel.game.players.PlayerBadge
import zel.game.players.inventory.BadgeHolders
import zel.game.players.inventory.BadgeTier
import zel.protocol.generated.PlayerBadgesOut
import zel.protocol.generated.PlayerBadgesOutCodec
import zel.protocol.outgoing.OutgoingMessage

class UserBadges(
    val badges: List<PlayerBadge>,
    val player: Int,
    private val ownerCounts: BadgeHolders,
) : OutgoingMessage<PlayerBadgesOut>(PlayerBadgesOutCodec) {
    override fun message() =
        PlayerBadgesOut(
            player,
            badges.map { badge ->
                val ownerCount = ownerCounts.count(badge.code)
                PlayerBadgesOut.List1Item(
                    badge.slot,
                    badge.code,
                    ownerCount,
                    BadgeTier.forHolders(ownerCount).code,
                )
            },
        )
}
