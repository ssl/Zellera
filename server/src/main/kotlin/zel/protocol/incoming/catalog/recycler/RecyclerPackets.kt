package zel.protocol.incoming.catalog.recycler

import zel.game.catalog.CatalogServices
import zel.protocol.generated.GetRecyclerPrizesInCodec
import zel.protocol.generated.GetRecyclerStatusInCodec
import zel.protocol.generated.RecycleItemsInCodec
import zel.protocol.generated.RecyclerPrizesOutCodec
import zel.protocol.outgoing.catalog.CatalogMessages
import zel.protocol.outgoing.catalog.CatalogReplies
import zel.protocol.typed.PacketPackage
import zel.protocol.typed.PacketRoute

class RecyclerPackets(
    services: CatalogServices,
    recycling: Recycling,
) : PacketPackage {
    override val name = "recycler"

    override val routes =
        listOf(
            PacketRoute(GetRecyclerPrizesInCodec) { s, _ ->
                s.send(
                    RecyclerPrizesOutCodec,
                    CatalogMessages.recyclerPrizes(
                        services.catalog.recycler.entries,
                    ) { level ->
                        services.catalog.recycler.rarityChance(level)
                    },
                )
            },
            PacketRoute(GetRecyclerStatusInCodec) { s, _ ->
                val status = services.recyclerCooldown.status(requireNotNull(s.client.player).profile.id)
                CatalogReplies.recyclerStatus(s.client, status[0], status[1])
            },
            PacketRoute(RecycleItemsInCodec) { s, m -> recycling.handle(s.client, m) },
        )
}
