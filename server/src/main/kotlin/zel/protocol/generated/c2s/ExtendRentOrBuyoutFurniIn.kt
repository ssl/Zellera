package zel.protocol.generated

data class ExtendRentOrBuyoutFurniIn(
    val isWallItem: Boolean,
    val itemId: Int,
    val buyout: Boolean,
)

object ExtendRentOrBuyoutFurniInCodec : IncomingCodec<ExtendRentOrBuyoutFurniIn> {
    override val name = "ExtendRentOrBuyoutFurniIn"

    override fun read(r: PacketReader): ExtendRentOrBuyoutFurniIn = readExtendRentOrBuyoutFurniIn(r)

    override fun write(w: PacketWriter, m: ExtendRentOrBuyoutFurniIn) = writeExtendRentOrBuyoutFurniIn(w, m)

    private fun readExtendRentOrBuyoutFurniIn(r: PacketReader): ExtendRentOrBuyoutFurniIn {
        val vIsWallItem = r.boolean()
        val vItemId = r.int()
        val vBuyout = r.boolean()
        return ExtendRentOrBuyoutFurniIn(vIsWallItem, vItemId, vBuyout)
    }

    private fun writeExtendRentOrBuyoutFurniIn(w: PacketWriter, m: ExtendRentOrBuyoutFurniIn) {
        w.boolean(m.isWallItem)
        w.int(m.itemId)
        w.boolean(m.buyout)
    }
}
