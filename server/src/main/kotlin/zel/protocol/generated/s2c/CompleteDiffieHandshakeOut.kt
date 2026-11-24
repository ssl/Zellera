package zel.protocol.generated

data class CompleteDiffieHandshakeOut(
    val string1: String,
    val trailing1: Boolean?,
)

object CompleteDiffieHandshakeOutCodec : OutgoingCodec<CompleteDiffieHandshakeOut> {
    override val name = "CompleteDiffieHandshakeOut"

    override fun read(r: PacketReader): CompleteDiffieHandshakeOut = readCompleteDiffieHandshakeOut(r)

    override fun write(w: PacketWriter, m: CompleteDiffieHandshakeOut) = writeCompleteDiffieHandshakeOut(w, m)

    private fun readCompleteDiffieHandshakeOut(r: PacketReader): CompleteDiffieHandshakeOut {
        val vString1 = r.string()
        val vTrailing1 = if (r.remaining > 0) r.boolean() else null
        return CompleteDiffieHandshakeOut(vString1, vTrailing1)
    }

    private fun writeCompleteDiffieHandshakeOut(w: PacketWriter, m: CompleteDiffieHandshakeOut) {
        w.string(m.string1)
        m.trailing1?.let { w.boolean(it) }
    }
}
