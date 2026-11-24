package zel.protocol.generated

data class GotMysteryBoxPrizeOut(
    val string1: String,
    val int1: Int,
)

object GotMysteryBoxPrizeOutCodec : OutgoingCodec<GotMysteryBoxPrizeOut> {
    override val name = "GotMysteryBoxPrizeOut"

    override fun read(r: PacketReader): GotMysteryBoxPrizeOut = readGotMysteryBoxPrizeOut(r)

    override fun write(w: PacketWriter, m: GotMysteryBoxPrizeOut) = writeGotMysteryBoxPrizeOut(w, m)

    private fun readGotMysteryBoxPrizeOut(r: PacketReader): GotMysteryBoxPrizeOut {
        val vString1 = r.string()
        val vInt1 = r.int()
        return GotMysteryBoxPrizeOut(vString1, vInt1)
    }

    private fun writeGotMysteryBoxPrizeOut(w: PacketWriter, m: GotMysteryBoxPrizeOut) {
        w.string(m.string1)
        w.int(m.int1)
    }
}
