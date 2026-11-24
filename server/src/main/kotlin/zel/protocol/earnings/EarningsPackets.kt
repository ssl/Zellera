package zel.protocol.earnings

import zel.game.earnings.IncomeRewards
import zel.protocol.generated.IncomeRewardClaimInCodec
import zel.protocol.generated.IncomeRewardStatusInCodec
import zel.protocol.typed.ClientCooldown
import zel.protocol.typed.PacketPackage
import zel.protocol.typed.PacketRoute

class EarningsPackets(
    rewards: IncomeRewards,
) : PacketPackage {
    private val statusCooldown = ClientCooldown("earnings.status", STATUS_GAP_MS)

    override val name = "earnings"
    override val routes =
        listOf(
            PacketRoute(IncomeRewardStatusInCodec) { session, _ ->
                if (!statusCooldown.acquire(session.client)) return@PacketRoute
                rewards.sendStatus(session.client.player)
            },
            PacketRoute(IncomeRewardClaimInCodec) { session, m ->
                val player = session.client.player ?: return@PacketRoute
                val category = m.byte1.toInt()
                if (category != IncomeRewards.ALL &&
                    (category < 0 || category > IncomeRewards.MAX_CATEGORY)
                ) {
                    return@PacketRoute
                }
                rewards.claim(player, category)
            },
        )

    private companion object {
        const val STATUS_GAP_MS = 250L
    }
}
