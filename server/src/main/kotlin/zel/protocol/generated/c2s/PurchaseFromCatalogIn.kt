package zel.protocol.generated

data class PurchaseFromCatalogIn(
    val int1: Int,
    val int2: Int,
    val string1: String,
    val int3: Int,
)

object PurchaseFromCatalogInCodec : IncomingCodec<PurchaseFromCatalogIn> {
    override val name = "PurchaseFromCatalogIn"

    override fun read(r: PacketReader): PurchaseFromCatalogIn = readPurchaseFromCatalogIn(r)

    override fun write(w: PacketWriter, m: PurchaseFromCatalogIn) = writePurchaseFromCatalogIn(w, m)

    private fun readPurchaseFromCatalogIn(r: PacketReader): PurchaseFromCatalogIn {
        val vInt1 = r.int()
        val vInt2 = r.int()
        val vString1 = r.string()
        val vInt3 = r.int()
        return PurchaseFromCatalogIn(vInt1, vInt2, vString1, vInt3)
    }

    private fun writePurchaseFromCatalogIn(w: PacketWriter, m: PurchaseFromCatalogIn) {
        w.int(m.int1)
        w.int(m.int2)
        w.string(m.string1)
        w.int(m.int3)
    }
}
