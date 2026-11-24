package zel.protocol.generated

data object GetMarketplaceConfigurationIn

object GetMarketplaceConfigurationInCodec : IncomingCodec<GetMarketplaceConfigurationIn> {
    override val name = "GetMarketplaceConfigurationIn"

    override fun read(r: PacketReader): GetMarketplaceConfigurationIn = GetMarketplaceConfigurationIn

    override fun write(w: PacketWriter, m: GetMarketplaceConfigurationIn) = writeGetMarketplaceConfigurationIn(w, m)

    @Suppress("UNUSED_PARAMETER")
    private fun writeGetMarketplaceConfigurationIn(w: PacketWriter, m: GetMarketplaceConfigurationIn) {
    }
}
