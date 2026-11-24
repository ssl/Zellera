package zel.protocol.generated

data class PurchaseBasicMembershipExtensionIn(
    val int1: Int,
)

object PurchaseBasicMembershipExtensionInCodec : IncomingCodec<PurchaseBasicMembershipExtensionIn> {
    override val name = "PurchaseBasicMembershipExtensionIn"

    override fun read(r: PacketReader): PurchaseBasicMembershipExtensionIn = readPurchaseBasicMembershipExtensionIn(r)

    override fun write(w: PacketWriter, m: PurchaseBasicMembershipExtensionIn) = writePurchaseBasicMembershipExtensionIn(w, m)

    private fun readPurchaseBasicMembershipExtensionIn(r: PacketReader): PurchaseBasicMembershipExtensionIn {
        val vInt1 = r.int()
        return PurchaseBasicMembershipExtensionIn(vInt1)
    }

    private fun writePurchaseBasicMembershipExtensionIn(w: PacketWriter, m: PurchaseBasicMembershipExtensionIn) {
        w.int(m.int1)
    }
}
