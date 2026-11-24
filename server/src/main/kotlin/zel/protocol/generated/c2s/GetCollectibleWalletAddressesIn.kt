package zel.protocol.generated

data object GetCollectibleWalletAddressesIn

object GetCollectibleWalletAddressesInCodec : IncomingCodec<GetCollectibleWalletAddressesIn> {
    override val name = "GetCollectibleWalletAddressesIn"

    override fun read(r: PacketReader): GetCollectibleWalletAddressesIn = GetCollectibleWalletAddressesIn

    override fun write(w: PacketWriter, m: GetCollectibleWalletAddressesIn) = writeGetCollectibleWalletAddressesIn(w, m)

    @Suppress("UNUSED_PARAMETER")
    private fun writeGetCollectibleWalletAddressesIn(w: PacketWriter, m: GetCollectibleWalletAddressesIn) {
    }
}
