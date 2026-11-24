package zel.protocol.generated

data class GuideReportingStatusOut(
    val int1: Int,
    val rest: ByteArray,
)

object GuideReportingStatusOutCodec : OutgoingCodec<GuideReportingStatusOut> {
    override val name = "GuideReportingStatusOut"

    override fun read(r: PacketReader): GuideReportingStatusOut = readGuideReportingStatusOut(r)

    override fun write(w: PacketWriter, m: GuideReportingStatusOut) = writeGuideReportingStatusOut(w, m)

    private fun readGuideReportingStatusOut(r: PacketReader): GuideReportingStatusOut {
        val vInt1 = r.int()
        val vRest = r.rest()
        return GuideReportingStatusOut(vInt1, vRest)
    }

    private fun writeGuideReportingStatusOut(w: PacketWriter, m: GuideReportingStatusOut) {
        w.int(m.int1)
        w.bytes(m.rest)
    }
}
