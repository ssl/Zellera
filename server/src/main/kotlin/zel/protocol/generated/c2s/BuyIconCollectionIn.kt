package zel.protocol.generated

data class BuyIconCollectionIn(
    val id: Int,
)

object BuyIconCollectionInCodec : IncomingCodec<BuyIconCollectionIn> {
    override val name = "BuyIconCollectionIn"

    override fun read(r: PacketReader): BuyIconCollectionIn = readBuyIconCollectionIn(r)

    override fun write(w: PacketWriter, m: BuyIconCollectionIn) = writeBuyIconCollectionIn(w, m)

    private fun readBuyIconCollectionIn(r: PacketReader): BuyIconCollectionIn {
        val vId = r.int()
        return BuyIconCollectionIn(vId)
    }

    private fun writeBuyIconCollectionIn(w: PacketWriter, m: BuyIconCollectionIn) {
        w.int(m.id)
    }
}
