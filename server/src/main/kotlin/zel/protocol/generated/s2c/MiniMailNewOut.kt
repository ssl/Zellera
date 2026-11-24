package zel.protocol.generated

data object MiniMailNewOut

object MiniMailNewOutCodec : OutgoingCodec<MiniMailNewOut> {
    override val name = "MiniMailNewOut"

    override fun read(r: PacketReader): MiniMailNewOut = MiniMailNewOut

    override fun write(w: PacketWriter, m: MiniMailNewOut) = writeMiniMailNewOut(w, m)

    @Suppress("UNUSED_PARAMETER")
    private fun writeMiniMailNewOut(w: PacketWriter, m: MiniMailNewOut) {
    }
}
