package zel.protocol.generated

data class WiredLinkSaveConfigurationIn(
    val furniId: Int,
    val networkId: String,
    val receiveLocal: Boolean,
)

object WiredLinkSaveConfigurationInCodec : IncomingCodec<WiredLinkSaveConfigurationIn> {
    override val name = "WiredLinkSaveConfigurationIn"

    override fun read(r: PacketReader): WiredLinkSaveConfigurationIn = readWiredLinkSaveConfigurationIn(r)

    override fun write(w: PacketWriter, m: WiredLinkSaveConfigurationIn) = writeWiredLinkSaveConfigurationIn(w, m)

    private fun readWiredLinkSaveConfigurationIn(r: PacketReader): WiredLinkSaveConfigurationIn {
        val vFurniId = r.int()
        val vNetworkId = r.string()
        val vReceiveLocal = r.boolean()
        return WiredLinkSaveConfigurationIn(vFurniId, vNetworkId, vReceiveLocal)
    }

    private fun writeWiredLinkSaveConfigurationIn(w: PacketWriter, m: WiredLinkSaveConfigurationIn) {
        w.int(m.furniId)
        w.string(m.networkId)
        w.boolean(m.receiveLocal)
    }
}
