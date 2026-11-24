package zel.protocol.generated

data class PurchasableChatStylesOut(
    val list1: List<Int>,
)

object PurchasableChatStylesOutCodec : OutgoingCodec<PurchasableChatStylesOut> {
    override val name = "PurchasableChatStylesOut"

    override fun read(r: PacketReader): PurchasableChatStylesOut = readPurchasableChatStylesOut(r)

    override fun write(w: PacketWriter, m: PurchasableChatStylesOut) = writePurchasableChatStylesOut(w, m)

    private fun readPurchasableChatStylesOut(r: PacketReader): PurchasableChatStylesOut {
        val vList1 = List(r.countInt(4)) { r.int() }
        return PurchasableChatStylesOut(vList1)
    }

    private fun writePurchasableChatStylesOut(w: PacketWriter, m: PurchasableChatStylesOut) {
        w.countInt(m.list1.size)
        m.list1.forEach { w.int(it) }
    }
}
