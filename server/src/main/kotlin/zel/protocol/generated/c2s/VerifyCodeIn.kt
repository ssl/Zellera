package zel.protocol.generated

data class VerifyCodeIn(
    val string1: String,
)

object VerifyCodeInCodec : IncomingCodec<VerifyCodeIn> {
    override val name = "VerifyCodeIn"

    override fun read(r: PacketReader): VerifyCodeIn = readVerifyCodeIn(r)

    override fun write(w: PacketWriter, m: VerifyCodeIn) = writeVerifyCodeIn(w, m)

    private fun readVerifyCodeIn(r: PacketReader): VerifyCodeIn {
        val vString1 = r.string()
        return VerifyCodeIn(vString1)
    }

    private fun writeVerifyCodeIn(w: PacketWriter, m: VerifyCodeIn) {
        w.string(m.string1)
    }
}
