package zel.protocol.generated

data class BuyIconIn(
    val id: Int,
)

object BuyIconInCodec : IncomingCodec<BuyIconIn> {
    override val name = "BuyIconIn"

    override fun read(r: PacketReader): BuyIconIn = readBuyIconIn(r)

    override fun write(w: PacketWriter, m: BuyIconIn) = writeBuyIconIn(w, m)

    private fun readBuyIconIn(r: PacketReader): BuyIconIn {
        val vId = r.int()
        return BuyIconIn(vId)
    }

    private fun writeBuyIconIn(w: PacketWriter, m: BuyIconIn) {
        w.int(m.id)
    }
}
