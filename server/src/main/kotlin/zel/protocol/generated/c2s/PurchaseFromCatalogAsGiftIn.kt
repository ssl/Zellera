package zel.protocol.generated

data class PurchaseFromCatalogAsGiftIn(
    val int1: Int,
    val int2: Int,
    val string1: String,
    val string2: String,
    val string3: String,
    val int3: Int,
    val int4: Int,
    val int5: Int,
    val boolean1: Boolean,
)

object PurchaseFromCatalogAsGiftInCodec : IncomingCodec<PurchaseFromCatalogAsGiftIn> {
    override val name = "PurchaseFromCatalogAsGiftIn"

    override fun read(r: PacketReader): PurchaseFromCatalogAsGiftIn = readPurchaseFromCatalogAsGiftIn(r)

    override fun write(w: PacketWriter, m: PurchaseFromCatalogAsGiftIn) = writePurchaseFromCatalogAsGiftIn(w, m)

    private fun readPurchaseFromCatalogAsGiftIn(r: PacketReader): PurchaseFromCatalogAsGiftIn {
        val vInt1 = r.int()
        val vInt2 = r.int()
        val vString1 = r.string()
        val vString2 = r.string()
        val vString3 = r.string()
        val vInt3 = r.int()
        val vInt4 = r.int()
        val vInt5 = r.int()
        val vBoolean1 = r.boolean()
        return PurchaseFromCatalogAsGiftIn(vInt1, vInt2, vString1, vString2, vString3, vInt3, vInt4, vInt5, vBoolean1)
    }

    private fun writePurchaseFromCatalogAsGiftIn(w: PacketWriter, m: PurchaseFromCatalogAsGiftIn) {
        w.int(m.int1)
        w.int(m.int2)
        w.string(m.string1)
        w.string(m.string2)
        w.string(m.string3)
        w.int(m.int3)
        w.int(m.int4)
        w.int(m.int5)
        w.boolean(m.boolean1)
    }
}
