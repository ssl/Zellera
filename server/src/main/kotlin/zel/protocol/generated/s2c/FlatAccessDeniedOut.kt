package zel.protocol.generated

data class FlatAccessDeniedOut(
    val int1: Int,
    val trailing1: String?,
)

object FlatAccessDeniedOutCodec : OutgoingCodec<FlatAccessDeniedOut> {
    override val name = "FlatAccessDeniedOut"

    override fun read(r: PacketReader): FlatAccessDeniedOut = readFlatAccessDeniedOut(r)

    override fun write(w: PacketWriter, m: FlatAccessDeniedOut) = writeFlatAccessDeniedOut(w, m)

    private fun readFlatAccessDeniedOut(r: PacketReader): FlatAccessDeniedOut {
        val vInt1 = r.int()
        val vTrailing1 = if (r.remaining > 0) r.string() else null
        return FlatAccessDeniedOut(vInt1, vTrailing1)
    }

    private fun writeFlatAccessDeniedOut(w: PacketWriter, m: FlatAccessDeniedOut) {
        w.int(m.int1)
        m.trailing1?.let { w.string(it) }
    }
}
