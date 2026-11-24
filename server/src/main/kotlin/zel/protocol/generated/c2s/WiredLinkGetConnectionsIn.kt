package zel.protocol.generated

data class WiredLinkGetConnectionsIn(
    val furniId: Int,
    val networkId: String,
)

object WiredLinkGetConnectionsInCodec : IncomingCodec<WiredLinkGetConnectionsIn> {
    override val name = "WiredLinkGetConnectionsIn"

    override fun read(r: PacketReader): WiredLinkGetConnectionsIn = readWiredLinkGetConnectionsIn(r)

    override fun write(w: PacketWriter, m: WiredLinkGetConnectionsIn) = writeWiredLinkGetConnectionsIn(w, m)

    private fun readWiredLinkGetConnectionsIn(r: PacketReader): WiredLinkGetConnectionsIn {
        val vFurniId = r.int()
        val vNetworkId = r.string()
        return WiredLinkGetConnectionsIn(vFurniId, vNetworkId)
    }

    private fun writeWiredLinkGetConnectionsIn(w: PacketWriter, m: WiredLinkGetConnectionsIn) {
        w.int(m.furniId)
        w.string(m.networkId)
    }
}
