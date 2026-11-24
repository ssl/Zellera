package zel.protocol.generated

data class LtdRaffleEnteredOut(
    val string1: String,
)

object LtdRaffleEnteredOutCodec : OutgoingCodec<LtdRaffleEnteredOut> {
    override val name = "LtdRaffleEnteredOut"

    override fun read(r: PacketReader): LtdRaffleEnteredOut = readLtdRaffleEnteredOut(r)

    override fun write(w: PacketWriter, m: LtdRaffleEnteredOut) = writeLtdRaffleEnteredOut(w, m)

    private fun readLtdRaffleEnteredOut(r: PacketReader): LtdRaffleEnteredOut {
        val vString1 = r.string()
        return LtdRaffleEnteredOut(vString1)
    }

    private fun writeLtdRaffleEnteredOut(w: PacketWriter, m: LtdRaffleEnteredOut) {
        w.string(m.string1)
    }
}
