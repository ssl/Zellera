package zel.protocol.generated

data object GetGuideReportingStatusIn

object GetGuideReportingStatusInCodec : IncomingCodec<GetGuideReportingStatusIn> {
    override val name = "GetGuideReportingStatusIn"

    override fun read(r: PacketReader): GetGuideReportingStatusIn = GetGuideReportingStatusIn

    override fun write(w: PacketWriter, m: GetGuideReportingStatusIn) = writeGetGuideReportingStatusIn(w, m)

    @Suppress("UNUSED_PARAMETER")
    private fun writeGetGuideReportingStatusIn(w: PacketWriter, m: GetGuideReportingStatusIn) {
    }
}
