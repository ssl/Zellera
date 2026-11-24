package zel.protocol.generated

data class CollectibleWalletAddressesOut(
    val string1: String,
    val list1: List<String>,
)

object CollectibleWalletAddressesOutCodec : OutgoingCodec<CollectibleWalletAddressesOut> {
    override val name = "CollectibleWalletAddressesOut"

    override fun read(r: PacketReader): CollectibleWalletAddressesOut = readCollectibleWalletAddressesOut(r)

    override fun write(w: PacketWriter, m: CollectibleWalletAddressesOut) = writeCollectibleWalletAddressesOut(w, m)

    private fun readCollectibleWalletAddressesOut(r: PacketReader): CollectibleWalletAddressesOut {
        val vString1 = r.string()
        val vList1 = List(r.countInt(2)) { r.string() }
        return CollectibleWalletAddressesOut(vString1, vList1)
    }

    private fun writeCollectibleWalletAddressesOut(w: PacketWriter, m: CollectibleWalletAddressesOut) {
        w.string(m.string1)
        w.countInt(m.list1.size)
        m.list1.forEach { w.string(it) }
    }
}
