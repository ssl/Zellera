package zel.protocol.generated

data class TreasureHuntUpdateOut(
    val string1: String,
    val int1: Int,
    val int2: Int,
    val boolean1: Boolean,
)

object TreasureHuntUpdateOutCodec : OutgoingCodec<TreasureHuntUpdateOut> {
    override val name = "TreasureHuntUpdateOut"

    override fun read(r: PacketReader): TreasureHuntUpdateOut = readTreasureHuntUpdateOut(r)

    override fun write(w: PacketWriter, m: TreasureHuntUpdateOut) = writeTreasureHuntUpdateOut(w, m)

    private fun readTreasureHuntUpdateOut(r: PacketReader): TreasureHuntUpdateOut {
        val vString1 = r.string()
        val vInt1 = r.int()
        val vInt2 = r.int()
        val vBoolean1 = r.boolean()
        return TreasureHuntUpdateOut(vString1, vInt1, vInt2, vBoolean1)
    }

    private fun writeTreasureHuntUpdateOut(w: PacketWriter, m: TreasureHuntUpdateOut) {
        w.string(m.string1)
        w.int(m.int1)
        w.int(m.int2)
        w.boolean(m.boolean1)
    }
}
