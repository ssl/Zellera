package zel.protocol.generated

data class NftStorePurchaseOut(
    val short1: Short,
)

object NftStorePurchaseOutCodec : OutgoingCodec<NftStorePurchaseOut> {
    override val name = "NftStorePurchaseOut"

    override fun read(r: PacketReader): NftStorePurchaseOut = readNftStorePurchaseOut(r)

    override fun write(w: PacketWriter, m: NftStorePurchaseOut) = writeNftStorePurchaseOut(w, m)

    private fun readNftStorePurchaseOut(r: PacketReader): NftStorePurchaseOut {
        val vShort1 = r.short()
        return NftStorePurchaseOut(vShort1)
    }

    private fun writeNftStorePurchaseOut(w: PacketWriter, m: NftStorePurchaseOut) {
        w.short(m.short1)
    }
}
