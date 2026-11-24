package zel.protocol.generated

data object UnusedEmptyIn

object UnusedEmptyInCodec : IncomingCodec<UnusedEmptyIn> {
    override val name = "UnusedEmptyIn"

    override fun read(r: PacketReader): UnusedEmptyIn = UnusedEmptyIn

    override fun write(w: PacketWriter, m: UnusedEmptyIn) = writeUnusedEmptyIn(w, m)

    @Suppress("UNUSED_PARAMETER")
    private fun writeUnusedEmptyIn(w: PacketWriter, m: UnusedEmptyIn) {
    }
}
