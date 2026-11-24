package zel.protocol.generated

data class WiredLinkDeleteNetworkIn(
    val furniId: Int,
    val networkId: String,
)

object WiredLinkDeleteNetworkInCodec : IncomingCodec<WiredLinkDeleteNetworkIn> {
    override val name = "WiredLinkDeleteNetworkIn"

    override fun read(r: PacketReader): WiredLinkDeleteNetworkIn = readWiredLinkDeleteNetworkIn(r)

    override fun write(w: PacketWriter, m: WiredLinkDeleteNetworkIn) = writeWiredLinkDeleteNetworkIn(w, m)

    private fun readWiredLinkDeleteNetworkIn(r: PacketReader): WiredLinkDeleteNetworkIn {
        val vFurniId = r.int()
        val vNetworkId = r.string()
        return WiredLinkDeleteNetworkIn(vFurniId, vNetworkId)
    }

    private fun writeWiredLinkDeleteNetworkIn(w: PacketWriter, m: WiredLinkDeleteNetworkIn) {
        w.int(m.furniId)
        w.string(m.networkId)
    }
}
