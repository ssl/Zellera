package zel.protocol.generated

data class PurchaseErrorOut(
    val int1: Int,
)

object PurchaseErrorOutCodec : OutgoingCodec<PurchaseErrorOut> {
    override val name = "PurchaseErrorOut"

    override fun read(r: PacketReader): PurchaseErrorOut = readPurchaseErrorOut(r)

    override fun write(w: PacketWriter, m: PurchaseErrorOut) = writePurchaseErrorOut(w, m)

    private fun readPurchaseErrorOut(r: PacketReader): PurchaseErrorOut {
        val vInt1 = r.int()
        return PurchaseErrorOut(vInt1)
    }

    private fun writePurchaseErrorOut(w: PacketWriter, m: PurchaseErrorOut) {
        w.int(m.int1)
    }
}
