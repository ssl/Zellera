package zel.protocol.generated

data class PurchaseMintTokenIn(
    val int1: Int,
    val string1: String,
)

object PurchaseMintTokenInCodec : IncomingCodec<PurchaseMintTokenIn> {
    override val name = "PurchaseMintTokenIn"

    override fun read(r: PacketReader): PurchaseMintTokenIn = readPurchaseMintTokenIn(r)

    override fun write(w: PacketWriter, m: PurchaseMintTokenIn) = writePurchaseMintTokenIn(w, m)

    private fun readPurchaseMintTokenIn(r: PacketReader): PurchaseMintTokenIn {
        val vInt1 = r.int()
        val vString1 = r.string()
        return PurchaseMintTokenIn(vInt1, vString1)
    }

    private fun writePurchaseMintTokenIn(w: PacketWriter, m: PurchaseMintTokenIn) {
        w.int(m.int1)
        w.string(m.string1)
    }
}
