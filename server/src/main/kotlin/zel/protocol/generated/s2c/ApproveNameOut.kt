package zel.protocol.generated

data class ApproveNameOut(
    val int1: Int,
    val string1: String,
)

object ApproveNameOutCodec : OutgoingCodec<ApproveNameOut> {
    override val name = "ApproveNameOut"

    override fun read(r: PacketReader): ApproveNameOut = readApproveNameOut(r)

    override fun write(w: PacketWriter, m: ApproveNameOut) = writeApproveNameOut(w, m)

    private fun readApproveNameOut(r: PacketReader): ApproveNameOut {
        val vInt1 = r.int()
        val vString1 = r.string()
        return ApproveNameOut(vInt1, vString1)
    }

    private fun writeApproveNameOut(w: PacketWriter, m: ApproveNameOut) {
        w.int(m.int1)
        w.string(m.string1)
    }
}
