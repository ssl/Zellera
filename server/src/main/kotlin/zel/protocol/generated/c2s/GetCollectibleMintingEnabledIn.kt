package zel.protocol.generated

data object GetCollectibleMintingEnabledIn

object GetCollectibleMintingEnabledInCodec : IncomingCodec<GetCollectibleMintingEnabledIn> {
    override val name = "GetCollectibleMintingEnabledIn"

    override fun read(r: PacketReader): GetCollectibleMintingEnabledIn = GetCollectibleMintingEnabledIn

    override fun write(w: PacketWriter, m: GetCollectibleMintingEnabledIn) = writeGetCollectibleMintingEnabledIn(w, m)

    @Suppress("UNUSED_PARAMETER")
    private fun writeGetCollectibleMintingEnabledIn(w: PacketWriter, m: GetCollectibleMintingEnabledIn) {
    }
}
