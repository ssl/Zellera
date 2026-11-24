package zel.protocol.generated

data object InitDiffieHandshakeIn

object InitDiffieHandshakeInCodec : IncomingCodec<InitDiffieHandshakeIn> {
    override val name = "InitDiffieHandshakeIn"

    override fun read(r: PacketReader): InitDiffieHandshakeIn = InitDiffieHandshakeIn

    override fun write(w: PacketWriter, m: InitDiffieHandshakeIn) = writeInitDiffieHandshakeIn(w, m)

    @Suppress("UNUSED_PARAMETER")
    private fun writeInitDiffieHandshakeIn(w: PacketWriter, m: InitDiffieHandshakeIn) {
    }
}
