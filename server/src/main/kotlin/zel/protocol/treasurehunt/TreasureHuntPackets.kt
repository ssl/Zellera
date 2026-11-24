package zel.protocol.treasurehunt

import zel.game.treasurehunt.TreasureHunts
import zel.protocol.generated.ProgressTreasureHuntInCodec
import zel.protocol.typed.PacketPackage
import zel.protocol.typed.PacketRoute

class TreasureHuntPackets(
    hunts: () -> TreasureHunts?,
    enabled: () -> Boolean,
) : PacketPackage {
    override val name = "treasure-hunt"

    override val routes =
        listOf(
            PacketRoute(ProgressTreasureHuntInCodec) { session, m ->
                val player = session.client.player ?: return@PacketRoute
                if (enabled()) hunts()?.takeStep(player, huntCode = m.string1, stepCode = m.string2)
            },
        )
}
