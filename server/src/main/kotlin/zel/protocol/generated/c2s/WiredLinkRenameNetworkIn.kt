package zel.protocol.generated

data class WiredLinkRenameNetworkIn(
    val furniId: Int,
    val networkId: String,
    val name: String,
)

object WiredLinkRenameNetworkInCodec : IncomingCodec<WiredLinkRenameNetworkIn> {
    override val name = "WiredLinkRenameNetworkIn"

    override fun read(r: PacketReader): WiredLinkRenameNetworkIn = readWiredLinkRenameNetworkIn(r)

    override fun write(w: PacketWriter, m: WiredLinkRenameNetworkIn) = writeWiredLinkRenameNetworkIn(w, m)

    private fun readWiredLinkRenameNetworkIn(r: PacketReader): WiredLinkRenameNetworkIn {
        val vFurniId = r.int()
        val vNetworkId = r.string()
        val vName = r.string()
        return WiredLinkRenameNetworkIn(vFurniId, vNetworkId, vName)
    }

    private fun writeWiredLinkRenameNetworkIn(w: PacketWriter, m: WiredLinkRenameNetworkIn) {
        w.int(m.furniId)
        w.string(m.networkId)
        w.string(m.name)
    }
}
