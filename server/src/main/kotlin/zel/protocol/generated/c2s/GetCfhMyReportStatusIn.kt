package zel.protocol.generated

data object GetCfhMyReportStatusIn

object GetCfhMyReportStatusInCodec : IncomingCodec<GetCfhMyReportStatusIn> {
    override val name = "GetCfhMyReportStatusIn"

    override fun read(r: PacketReader): GetCfhMyReportStatusIn = GetCfhMyReportStatusIn

    override fun write(w: PacketWriter, m: GetCfhMyReportStatusIn) = writeGetCfhMyReportStatusIn(w, m)

    @Suppress("UNUSED_PARAMETER")
    private fun writeGetCfhMyReportStatusIn(w: PacketWriter, m: GetCfhMyReportStatusIn) {
    }
}
