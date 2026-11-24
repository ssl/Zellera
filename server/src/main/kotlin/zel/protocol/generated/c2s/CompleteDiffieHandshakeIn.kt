package zel.protocol.generated

data class CompleteDiffieHandshakeIn(
    val sharedKey: String,
)

object CompleteDiffieHandshakeInCodec : IncomingCodec<CompleteDiffieHandshakeIn> {
    override val name = "CompleteDiffieHandshakeIn"

    override fun read(r: PacketReader): CompleteDiffieHandshakeIn = readCompleteDiffieHandshakeIn(r)

    override fun write(w: PacketWriter, m: CompleteDiffieHandshakeIn) = writeCompleteDiffieHandshakeIn(w, m)

    private fun readCompleteDiffieHandshakeIn(r: PacketReader): CompleteDiffieHandshakeIn {
        val vSharedKey = r.string()
        return CompleteDiffieHandshakeIn(vSharedKey)
    }

    private fun writeCompleteDiffieHandshakeIn(w: PacketWriter, m: CompleteDiffieHandshakeIn) {
        w.string(m.sharedKey)
    }
}
