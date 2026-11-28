package zel.protocol.misc

import zel.protocol.generated.LatencyPingReportInCodec
import zel.protocol.generated.LatencyPingRequestInCodec
import zel.protocol.generated.LatencyPingResponseOut
import zel.protocol.generated.LatencyPingResponseOutCodec
import zel.protocol.generated.PongInCodec
import zel.protocol.typed.PacketPackage
import zel.protocol.typed.PacketRoute
import zel.util.Stir

class LatencyPackets(
    nowMillis: () -> Long = System::currentTimeMillis,
) : PacketPackage {
    override val name = "latency"

    override val routes =
        listOf(
            PacketRoute(PongInCodec) { _, _ -> },
            PacketRoute(LatencyPingRequestInCodec, requiresPlayer = false) { session, request ->
                val heard = session.client.player != null || request.int1 == Stir.door
                if (heard && session.client.latency.mayAnswerRequest(nowMillis(), MIN_ANSWER_GAP_MILLIS)) {
                    val answer = if (request.int1 == Stir.door) Stir.echo else request.int1
                    session.send(LatencyPingResponseOutCodec, LatencyPingResponseOut(answer))
                }
            },
            PacketRoute(LatencyPingReportInCodec) { _, _ -> },
        )

    private companion object {
        const val MIN_ANSWER_GAP_MILLIS = 4_000L
    }
}
