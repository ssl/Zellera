package zel.protocol.generated

data class WiredLinkCreateNetworkIn(
    val furniId: Int,
    val name: String,
)

object WiredLinkCreateNetworkInCodec : IncomingCodec<WiredLinkCreateNetworkIn> {
    override val name = "WiredLinkCreateNetworkIn"

    override fun read(r: PacketReader): WiredLinkCreateNetworkIn = readWiredLinkCreateNetworkIn(r)

    override fun write(w: PacketWriter, m: WiredLinkCreateNetworkIn) = writeWiredLinkCreateNetworkIn(w, m)

    private fun readWiredLinkCreateNetworkIn(r: PacketReader): WiredLinkCreateNetworkIn {
        val vFurniId = r.int()
        val vName = r.string()
        return WiredLinkCreateNetworkIn(vFurniId, vName)
    }

    private fun writeWiredLinkCreateNetworkIn(w: PacketWriter, m: WiredLinkCreateNetworkIn) {
        w.int(m.furniId)
        w.string(m.name)
    }
}
