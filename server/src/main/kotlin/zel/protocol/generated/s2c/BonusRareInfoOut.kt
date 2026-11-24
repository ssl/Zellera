package zel.protocol.generated

data class BonusRareInfoOut(
    val string1: String,
    val int1: Int,
    val int2: Int,
    val int3: Int,
)

object BonusRareInfoOutCodec : OutgoingCodec<BonusRareInfoOut> {
    override val name = "BonusRareInfoOut"

    override fun read(r: PacketReader): BonusRareInfoOut = readBonusRareInfoOut(r)

    override fun write(w: PacketWriter, m: BonusRareInfoOut) = writeBonusRareInfoOut(w, m)

    private fun readBonusRareInfoOut(r: PacketReader): BonusRareInfoOut {
        val vString1 = r.string()
        val vInt1 = r.int()
        val vInt2 = r.int()
        val vInt3 = r.int()
        return BonusRareInfoOut(vString1, vInt1, vInt2, vInt3)
    }

    private fun writeBonusRareInfoOut(w: PacketWriter, m: BonusRareInfoOut) {
        w.string(m.string1)
        w.int(m.int1)
        w.int(m.int2)
        w.int(m.int3)
    }
}
