package zel.protocol.generated

data object PongIn

object PongInCodec : IncomingCodec<PongIn> {
    override val name = "PongIn"

    override fun read(r: PacketReader): PongIn = PongIn

    override fun write(w: PacketWriter, m: PongIn) = writePongIn(w, m)

    @Suppress("UNUSED_PARAMETER")
    private fun writePongIn(w: PacketWriter, m: PongIn) {
    }
}
