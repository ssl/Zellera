package zel.protocol.generated

data class GuideSessionReportIn(
    val message: String,
)

object GuideSessionReportInCodec : IncomingCodec<GuideSessionReportIn> {
    override val name = "GuideSessionReportIn"

    override fun read(r: PacketReader): GuideSessionReportIn = readGuideSessionReportIn(r)

    override fun write(w: PacketWriter, m: GuideSessionReportIn) = writeGuideSessionReportIn(w, m)

    private fun readGuideSessionReportIn(r: PacketReader): GuideSessionReportIn {
        val vMessage = r.string()
        return GuideSessionReportIn(vMessage)
    }

    private fun writeGuideSessionReportIn(w: PacketWriter, m: GuideSessionReportIn) {
        w.string(m.message)
    }
}
