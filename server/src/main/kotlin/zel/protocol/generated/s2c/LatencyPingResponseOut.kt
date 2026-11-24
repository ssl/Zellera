package zel.protocol.generated

data class LatencyPingResponseOut(
    val int1: Int,
)

object LatencyPingResponseOutCodec : OutgoingCodec<LatencyPingResponseOut> {
    override val name = "LatencyPingResponseOut"

    override fun read(r: PacketReader): LatencyPingResponseOut = readLatencyPingResponseOut(r)

    override fun write(w: PacketWriter, m: LatencyPingResponseOut) = writeLatencyPingResponseOut(w, m)

    private fun readLatencyPingResponseOut(r: PacketReader): LatencyPingResponseOut {
        val vInt1 = r.int()
        return LatencyPingResponseOut(vInt1)
    }

    private fun writeLatencyPingResponseOut(w: PacketWriter, m: LatencyPingResponseOut) {
        w.int(m.int1)
    }
}
