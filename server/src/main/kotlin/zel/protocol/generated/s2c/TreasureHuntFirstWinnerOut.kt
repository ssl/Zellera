package zel.protocol.generated

data class TreasureHuntFirstWinnerOut(
    val string1: String,
    val int1: Int,
    val string2: String,
    val string3: String,
    val string4: String,
)

object TreasureHuntFirstWinnerOutCodec : OutgoingCodec<TreasureHuntFirstWinnerOut> {
    override val name = "TreasureHuntFirstWinnerOut"

    override fun read(r: PacketReader): TreasureHuntFirstWinnerOut = readTreasureHuntFirstWinnerOut(r)

    override fun write(w: PacketWriter, m: TreasureHuntFirstWinnerOut) = writeTreasureHuntFirstWinnerOut(w, m)

    private fun readTreasureHuntFirstWinnerOut(r: PacketReader): TreasureHuntFirstWinnerOut {
        val vString1 = r.string()
        val vInt1 = r.int()
        val vString2 = r.string()
        val vString3 = r.string()
        val vString4 = r.string()
        return TreasureHuntFirstWinnerOut(vString1, vInt1, vString2, vString3, vString4)
    }

    private fun writeTreasureHuntFirstWinnerOut(w: PacketWriter, m: TreasureHuntFirstWinnerOut) {
        w.string(m.string1)
        w.int(m.int1)
        w.string(m.string2)
        w.string(m.string3)
        w.string(m.string4)
    }
}
