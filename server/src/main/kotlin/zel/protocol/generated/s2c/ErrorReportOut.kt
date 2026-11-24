package zel.protocol.generated

data class ErrorReportOut(
    val int1: Int,
    val int2: Int,
    val string1: String,
)

object ErrorReportOutCodec : OutgoingCodec<ErrorReportOut> {
    override val name = "ErrorReportOut"

    override fun read(r: PacketReader): ErrorReportOut = readErrorReportOut(r)

    override fun write(w: PacketWriter, m: ErrorReportOut) = writeErrorReportOut(w, m)

    private fun readErrorReportOut(r: PacketReader): ErrorReportOut {
        val vInt1 = r.int()
        val vInt2 = r.int()
        val vString1 = r.string()
        return ErrorReportOut(vInt1, vInt2, vString1)
    }

    private fun writeErrorReportOut(w: PacketWriter, m: ErrorReportOut) {
        w.int(m.int1)
        w.int(m.int2)
        w.string(m.string1)
    }
}
