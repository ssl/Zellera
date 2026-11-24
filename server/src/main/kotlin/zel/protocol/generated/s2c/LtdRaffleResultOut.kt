package zel.protocol.generated

data class LtdRaffleResultOut(
    val string1: String,
    val byte1: Byte,
)

object LtdRaffleResultOutCodec : OutgoingCodec<LtdRaffleResultOut> {
    override val name = "LtdRaffleResultOut"

    override fun read(r: PacketReader): LtdRaffleResultOut = readLtdRaffleResultOut(r)

    override fun write(w: PacketWriter, m: LtdRaffleResultOut) = writeLtdRaffleResultOut(w, m)

    private fun readLtdRaffleResultOut(r: PacketReader): LtdRaffleResultOut {
        val vString1 = r.string()
        val vByte1 = r.byte()
        return LtdRaffleResultOut(vString1, vByte1)
    }

    private fun writeLtdRaffleResultOut(w: PacketWriter, m: LtdRaffleResultOut) {
        w.string(m.string1)
        w.byte(m.byte1)
    }
}
