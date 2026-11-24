package zel.protocol.generated

data class NftStorePurchaseIn(
    val string1: String,
    val string2: String,
)

object NftStorePurchaseInCodec : IncomingCodec<NftStorePurchaseIn> {
    override val name = "NftStorePurchaseIn"

    override fun read(r: PacketReader): NftStorePurchaseIn = readNftStorePurchaseIn(r)

    override fun write(w: PacketWriter, m: NftStorePurchaseIn) = writeNftStorePurchaseIn(w, m)

    private fun readNftStorePurchaseIn(r: PacketReader): NftStorePurchaseIn {
        val vString1 = r.string()
        val vString2 = r.string()
        return NftStorePurchaseIn(vString1, vString2)
    }

    private fun writeNftStorePurchaseIn(w: PacketWriter, m: NftStorePurchaseIn) {
        w.string(m.string1)
        w.string(m.string2)
    }
}
