package zel.protocol.generated

data class RoomAdPurchaseInfoOut(
    val boolean1: Boolean,
    val list1: List<List1Item>,
) {
    data class List1Item(
        val int1: Int,
        val string1: String,
        val boolean1: Boolean,
    )
}

object RoomAdPurchaseInfoOutCodec : OutgoingCodec<RoomAdPurchaseInfoOut> {
    override val name = "RoomAdPurchaseInfoOut"

    override fun read(r: PacketReader): RoomAdPurchaseInfoOut = readRoomAdPurchaseInfoOut(r)

    override fun write(w: PacketWriter, m: RoomAdPurchaseInfoOut) = writeRoomAdPurchaseInfoOut(w, m)

    private fun readRoomAdPurchaseInfoOutList1Item(r: PacketReader): RoomAdPurchaseInfoOut.List1Item {
        val vInt1 = r.int()
        val vString1 = r.string()
        val vBoolean1 = r.boolean()
        return RoomAdPurchaseInfoOut.List1Item(vInt1, vString1, vBoolean1)
    }

    private fun readRoomAdPurchaseInfoOut(r: PacketReader): RoomAdPurchaseInfoOut {
        val vBoolean1 = r.boolean()
        val vList1 = List(r.countInt(7)) { readRoomAdPurchaseInfoOutList1Item(r) }
        return RoomAdPurchaseInfoOut(vBoolean1, vList1)
    }

    private fun writeRoomAdPurchaseInfoOutList1Item(w: PacketWriter, m: RoomAdPurchaseInfoOut.List1Item) {
        w.int(m.int1)
        w.string(m.string1)
        w.boolean(m.boolean1)
    }

    private fun writeRoomAdPurchaseInfoOut(w: PacketWriter, m: RoomAdPurchaseInfoOut) {
        w.boolean(m.boolean1)
        w.countInt(m.list1.size)
        m.list1.forEach { writeRoomAdPurchaseInfoOutList1Item(w, it) }
    }
}
