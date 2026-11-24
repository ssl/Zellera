package zel.protocol.generated

data class PurchaseVipMembershipExtensionIn(
    val offerId: Int,
)

object PurchaseVipMembershipExtensionInCodec : IncomingCodec<PurchaseVipMembershipExtensionIn> {
    override val name = "PurchaseVipMembershipExtensionIn"

    override fun read(r: PacketReader): PurchaseVipMembershipExtensionIn = readPurchaseVipMembershipExtensionIn(r)

    override fun write(w: PacketWriter, m: PurchaseVipMembershipExtensionIn) = writePurchaseVipMembershipExtensionIn(w, m)

    private fun readPurchaseVipMembershipExtensionIn(r: PacketReader): PurchaseVipMembershipExtensionIn {
        val vOfferId = r.int()
        return PurchaseVipMembershipExtensionIn(vOfferId)
    }

    private fun writePurchaseVipMembershipExtensionIn(w: PacketWriter, m: PurchaseVipMembershipExtensionIn) {
        w.int(m.offerId)
    }
}
