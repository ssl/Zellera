package zel.protocol.generated

data class MakeOfferIn(
    val price: Int,
    val furniType: Int,
    val itemIds: List<Int>,
)

object MakeOfferInCodec : IncomingCodec<MakeOfferIn> {
    override val name = "MakeOfferIn"

    override fun read(r: PacketReader): MakeOfferIn = readMakeOfferIn(r)

    override fun write(w: PacketWriter, m: MakeOfferIn) = writeMakeOfferIn(w, m)

    private fun readMakeOfferIn(r: PacketReader): MakeOfferIn {
        val vPrice = r.int()
        val vFurniType = r.int()
        val vItemIds = List(r.countInt(4)) { r.int() }
        return MakeOfferIn(vPrice, vFurniType, vItemIds)
    }

    private fun writeMakeOfferIn(w: PacketWriter, m: MakeOfferIn) {
        w.int(m.price)
        w.int(m.furniType)
        w.countInt(m.itemIds.size)
        m.itemIds.forEach { w.int(it) }
    }
}
