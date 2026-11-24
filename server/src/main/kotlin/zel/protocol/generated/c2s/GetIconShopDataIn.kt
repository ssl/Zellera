package zel.protocol.generated

data object GetIconShopDataIn

object GetIconShopDataInCodec : IncomingCodec<GetIconShopDataIn> {
    override val name = "GetIconShopDataIn"

    override fun read(r: PacketReader): GetIconShopDataIn = GetIconShopDataIn

    override fun write(w: PacketWriter, m: GetIconShopDataIn) = writeGetIconShopDataIn(w, m)

    @Suppress("UNUSED_PARAMETER")
    private fun writeGetIconShopDataIn(w: PacketWriter, m: GetIconShopDataIn) {
    }
}
