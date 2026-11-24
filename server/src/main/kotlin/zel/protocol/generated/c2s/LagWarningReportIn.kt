package zel.protocol.generated

data class LagWarningReportIn(
    val int1: Int,
)

object LagWarningReportInCodec : IncomingCodec<LagWarningReportIn> {
    override val name = "LagWarningReportIn"

    override fun read(r: PacketReader): LagWarningReportIn = readLagWarningReportIn(r)

    override fun write(w: PacketWriter, m: LagWarningReportIn) = writeLagWarningReportIn(w, m)

    private fun readLagWarningReportIn(r: PacketReader): LagWarningReportIn {
        val vInt1 = r.int()
        return LagWarningReportIn(vInt1)
    }

    private fun writeLagWarningReportIn(w: PacketWriter, m: LagWarningReportIn) {
        w.int(m.int1)
    }
}
