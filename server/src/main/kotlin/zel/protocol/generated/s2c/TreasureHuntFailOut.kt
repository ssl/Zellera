package zel.protocol.generated

data class TreasureHuntFailOut(
    val string1: String,
    val int1: Int,
    val int2: Int,
)

object TreasureHuntFailOutCodec : OutgoingCodec<TreasureHuntFailOut> {
    override val name = "TreasureHuntFailOut"

    override fun read(r: PacketReader): TreasureHuntFailOut = readTreasureHuntFailOut(r)

    override fun write(w: PacketWriter, m: TreasureHuntFailOut) = writeTreasureHuntFailOut(w, m)

    private fun readTreasureHuntFailOut(r: PacketReader): TreasureHuntFailOut {
        val vString1 = r.string()
        val vInt1 = r.int()
        val vInt2 = r.int()
        return TreasureHuntFailOut(vString1, vInt1, vInt2)
    }

    private fun writeTreasureHuntFailOut(w: PacketWriter, m: TreasureHuntFailOut) {
        w.string(m.string1)
        w.int(m.int1)
        w.int(m.int2)
    }
}
