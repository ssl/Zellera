package zel.protocol.rewardtracks

import zel.game.rewardtracks.RewardTrackService
import zel.protocol.generated.ClaimRewardTrackPrizeInCodec
import zel.protocol.generated.PurchaseRewardTrackPremiumInCodec
import zel.protocol.typed.PacketPackage
import zel.protocol.typed.PacketRoute

class RewardTrackPackets(
    tracks: RewardTrackService,
) : PacketPackage {
    override val name = "rewardtracks"

    override val routes =
        listOf(
            PacketRoute(ClaimRewardTrackPrizeInCodec) { session, m ->
                tracks.claim(requireNotNull(session.client.player), m.trackId, m.prizeId)
            },
            PacketRoute(PurchaseRewardTrackPremiumInCodec) { session, m ->
                tracks.buyPass(requireNotNull(session.client.player), m.string1)
            },
        )
}
