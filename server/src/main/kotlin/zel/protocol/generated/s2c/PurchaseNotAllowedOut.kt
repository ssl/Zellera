package zel.protocol.generated

data class PurchaseNotAllowedOut(
    val int1: Int,
)

object PurchaseNotAllowedOutCodec : OutgoingCodec<PurchaseNotAllowedOut> {
    override val name = "PurchaseNotAllowedOut"

    override fun read(r: PacketReader): PurchaseNotAllowedOut = readPurchaseNotAllowedOut(r)

    override fun write(w: PacketWriter, m: PurchaseNotAllowedOut) = writePurchaseNotAllowedOut(w, m)

    private fun readPurchaseNotAllowedOut(r: PacketReader): PurchaseNotAllowedOut {
        val vInt1 = r.int()
        return PurchaseNotAllowedOut(vInt1)
    }

    private fun writePurchaseNotAllowedOut(w: PacketWriter, m: PurchaseNotAllowedOut) {
        w.int(m.int1)
    }
}
