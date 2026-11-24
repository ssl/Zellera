package zel.protocol.generated

data class LatencyPingReportIn(
    val int1: Int,
    val int2: Int,
    val int3: Int,
)

object LatencyPingReportInCodec : IncomingCodec<LatencyPingReportIn> {
    override val name = "LatencyPingReportIn"

    override fun read(r: PacketReader): LatencyPingReportIn = readLatencyPingReportIn(r)

    override fun write(w: PacketWriter, m: LatencyPingReportIn) = writeLatencyPingReportIn(w, m)

    private fun readLatencyPingReportIn(r: PacketReader): LatencyPingReportIn {
        val vInt1 = r.int()
        val vInt2 = r.int()
        val vInt3 = r.int()
        return LatencyPingReportIn(vInt1, vInt2, vInt3)
    }

    private fun writeLatencyPingReportIn(w: PacketWriter, m: LatencyPingReportIn) {
        w.int(m.int1)
        w.int(m.int2)
        w.int(m.int3)
    }
}
