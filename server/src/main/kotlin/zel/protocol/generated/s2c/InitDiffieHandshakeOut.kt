package zel.protocol.generated

data class InitDiffieHandshakeOut(
    val string1: String,
    val string2: String,
)

object InitDiffieHandshakeOutCodec : OutgoingCodec<InitDiffieHandshakeOut> {
    override val name = "InitDiffieHandshakeOut"

    override fun read(r: PacketReader): InitDiffieHandshakeOut = readInitDiffieHandshakeOut(r)

    override fun write(w: PacketWriter, m: InitDiffieHandshakeOut) = writeInitDiffieHandshakeOut(w, m)

    private fun readInitDiffieHandshakeOut(r: PacketReader): InitDiffieHandshakeOut {
        val vString1 = r.string()
        val vString2 = r.string()
        return InitDiffieHandshakeOut(vString1, vString2)
    }

    private fun writeInitDiffieHandshakeOut(w: PacketWriter, m: InitDiffieHandshakeOut) {
        w.string(m.string1)
        w.string(m.string2)
    }
}
