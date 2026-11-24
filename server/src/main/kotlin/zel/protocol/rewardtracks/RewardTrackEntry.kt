package zel.protocol.rewardtracks

import zel.game.rewardtracks.PremiumOffer
import zel.game.rewardtracks.RewardTrackDefinition

/** [offer] is the pass the window sells, none when the lane is open. */
data class RewardTrackEntry(
    val track: RewardTrackDefinition,
    val points: Int,
    val premium: Boolean,
    val progress: Map<String, Int>,
    val claimed: Set<String>,
    val offer: PremiumOffer? = null,
)
