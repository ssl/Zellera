package zel.protocol.featuredlooks

import zel.game.featuredlooks.FeaturedLooks
import zel.protocol.generated.FeaturedLooksOut
import zel.protocol.generated.FeaturedLooksOutCodec
import zel.protocol.generated.GetFeaturedLooksInCodec
import zel.protocol.outgoing.handshake.ErrorReport
import zel.protocol.typed.ClientCooldown
import zel.protocol.typed.PacketPackage
import zel.protocol.typed.PacketRoute

class FeaturedLooksPackets(
    featuredLooks: FeaturedLooks,
) : PacketPackage {
    private val cooldown = ClientCooldown("featuredlooks.get", RATE_LIMIT_MS)

    override val name = "featuredlooks"
    override val routes =
        listOf(
            PacketRoute(GetFeaturedLooksInCodec) { session, m ->
                val client = session.client
                if (!cooldown.acquire(client)) return@PacketRoute
                if (m.count < 0) {
                    client.send(ErrorReport.forRequest(NEGATIVE_COUNT_ERROR))
                    return@PacketRoute
                }
                val looks =
                    featuredLooks.draw(m.count.toInt()).map {
                        FeaturedLooksOut.List1Item(it.genderCode, it.figure)
                    }
                session.send(FeaturedLooksOutCodec, FeaturedLooksOut(looks))
            },
        )

    companion object {
        const val NEGATIVE_COUNT_ERROR = 900

        const val RATE_LIMIT_MS = 500L
    }
}
