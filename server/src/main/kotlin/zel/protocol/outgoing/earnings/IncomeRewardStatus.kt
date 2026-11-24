package zel.protocol.outgoing.earnings

import zel.game.earnings.IncomeRewards
import zel.protocol.generated.IncomeRewardStatusOut
import zel.protocol.generated.IncomeRewardStatusOutCodec
import zel.protocol.outgoing.OutgoingMessage

class IncomeRewardStatus(
    rewards: List<IncomeRewards.Reward>?,
) : OutgoingMessage<IncomeRewardStatusOut>(IncomeRewardStatusOutCodec) {
    private val rewards = rewards.orEmpty().toList()

    override fun message() =
        IncomeRewardStatusOut(
            rewards.map {
                IncomeRewardStatusOut.List1Item(
                    it.category.toByte(),
                    it.type.toByte(),
                    it.amount,
                    it.productCode,
                )
            },
        )
}
