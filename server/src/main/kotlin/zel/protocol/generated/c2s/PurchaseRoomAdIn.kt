package zel.protocol.generated

data class PurchaseRoomAdIn(
    val pageId: Int,
    val itemId: Int,
    val roomId: Int,
    val title: String,
    val boolean1: Boolean,
    val description: String,
    val categoryId: Int,
)

object PurchaseRoomAdInCodec : IncomingCodec<PurchaseRoomAdIn> {
    override val name = "PurchaseRoomAdIn"

    override fun read(r: PacketReader): PurchaseRoomAdIn = readPurchaseRoomAdIn(r)

    override fun write(w: PacketWriter, m: PurchaseRoomAdIn) = writePurchaseRoomAdIn(w, m)

    private fun readPurchaseRoomAdIn(r: PacketReader): PurchaseRoomAdIn {
        val vPageId = r.int()
        val vItemId = r.int()
        val vRoomId = r.int()
        val vTitle = r.string()
        val vBoolean1 = r.boolean()
        val vDescription = r.string()
        val vCategoryId = r.int()
        return PurchaseRoomAdIn(vPageId, vItemId, vRoomId, vTitle, vBoolean1, vDescription, vCategoryId)
    }

    private fun writePurchaseRoomAdIn(w: PacketWriter, m: PurchaseRoomAdIn) {
        w.int(m.pageId)
        w.int(m.itemId)
        w.int(m.roomId)
        w.string(m.title)
        w.boolean(m.boolean1)
        w.string(m.description)
        w.int(m.categoryId)
    }
}
