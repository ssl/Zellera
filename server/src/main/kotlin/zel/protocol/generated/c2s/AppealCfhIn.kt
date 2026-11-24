package zel.protocol.generated

data class AppealCfhIn(
    val reportId: Int,
)

object AppealCfhInCodec : IncomingCodec<AppealCfhIn> {
    override val name = "AppealCfhIn"

    override fun read(r: PacketReader): AppealCfhIn = readAppealCfhIn(r)

    override fun write(w: PacketWriter, m: AppealCfhIn) = writeAppealCfhIn(w, m)

    private fun readAppealCfhIn(r: PacketReader): AppealCfhIn {
        val vReportId = r.int()
        return AppealCfhIn(vReportId)
    }

    private fun writeAppealCfhIn(w: PacketWriter, m: AppealCfhIn) {
        w.int(m.reportId)
    }
}
