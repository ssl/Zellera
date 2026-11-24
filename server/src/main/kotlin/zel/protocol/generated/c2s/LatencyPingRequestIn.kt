package zel.protocol.generated

data class LatencyPingRequestIn(
    val int1: Int,
)

object LatencyPingRequestInCodec : IncomingCodec<LatencyPingRequestIn> {
    override val name = "LatencyPingRequestIn"

    override fun read(r: PacketReader): LatencyPingRequestIn = readLatencyPingRequestIn(r)

    override fun write(w: PacketWriter, m: LatencyPingRequestIn) = writeLatencyPingRequestIn(w, m)

    private fun readLatencyPingRequestIn(r: PacketReader): LatencyPingRequestIn {
        val vInt1 = r.int()
        return LatencyPingRequestIn(vInt1)
    }

    private fun writeLatencyPingRequestIn(w: PacketWriter, m: LatencyPingRequestIn) {
        w.int(m.int1)
    }
}
