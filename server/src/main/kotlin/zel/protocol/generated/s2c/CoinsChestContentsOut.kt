package zel.protocol.generated

data class CoinsChestContentsOut(
    val int1: Int,
    val int2: Int,
    val boolean1: Boolean,
)

object CoinsChestContentsOutCodec : OutgoingCodec<CoinsChestContentsOut> {
    override val name = "CoinsChestContentsOut"

    override fun read(r: PacketReader): CoinsChestContentsOut = readCoinsChestContentsOut(r)

    override fun write(w: PacketWriter, m: CoinsChestContentsOut) = writeCoinsChestContentsOut(w, m)

    private fun readCoinsChestContentsOut(r: PacketReader): CoinsChestContentsOut {
        val vInt1 = r.int()
        val vInt2 = r.int()
        val vBoolean1 = r.boolean()
        return CoinsChestContentsOut(vInt1, vInt2, vBoolean1)
    }

    private fun writeCoinsChestContentsOut(w: PacketWriter, m: CoinsChestContentsOut) {
        w.int(m.int1)
        w.int(m.int2)
        w.boolean(m.boolean1)
    }
}
