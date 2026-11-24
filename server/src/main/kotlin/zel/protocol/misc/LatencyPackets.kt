package zel.protocol.misc

import zel.protocol.generated.LatencyPingReportInCodec
import zel.protocol.generated.LatencyPingRequestInCodec
import zel.protocol.generated.LatencyPingResponseOut
import zel.protocol.generated.LatencyPingResponseOutCodec
import zel.protocol.generated.PongInCodec
import zel.protocol.typed.PacketPackage
import zel.protocol.typed.PacketRoute

class LatencyPackets(
    nowMillis: () -> Long = System::currentTimeMillis,
) : PacketPackage {
    override val name = "latency"

    override val routes =
        listOf(
            PacketRoute(PongInCodec) { _, _ -> },
            PacketRoute(LatencyPingRequestInCodec) { session, request ->
                if (session.client.latency.mayAnswerRequest(nowMillis(), MIN_ANSWER_GAP_MILLIS)) {
                    session.send(LatencyPingResponseOutCodec, LatencyPingResponseOut(request.int1))
                }
            },
            PacketRoute(LatencyPingReportInCodec) { _, _ -> },
        )

    private companion object {
        const val MIN_ANSWER_GAP_MILLIS = 4_000L
    }
}
