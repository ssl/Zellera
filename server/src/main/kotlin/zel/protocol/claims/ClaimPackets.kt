package zel.protocol.claims

import zel.game.claims.ProductClaims
import zel.protocol.generated.ClaimProductInCodec
import zel.protocol.generated.HasClaimedProductInCodec
import zel.protocol.typed.PacketPackage
import zel.protocol.typed.PacketRoute

class ClaimPackets(
    claims: ProductClaims,
    messages: ClaimMessages,
) : PacketPackage {
    override val name = "claims"

    override val routes =
        listOf(
            PacketRoute(ClaimProductInCodec) { session, m ->
                claims.claim(requireNotNull(session.client.player), m.claimId, m.choice ?: 0)
            },
            PacketRoute(HasClaimedProductInCodec) { session, m ->
                val player = requireNotNull(session.client.player)
                messages.hasClaimed(session.client, m.claimId, claims.alreadyTaken(player.profile.id, m.claimId))
            },
        )
}
