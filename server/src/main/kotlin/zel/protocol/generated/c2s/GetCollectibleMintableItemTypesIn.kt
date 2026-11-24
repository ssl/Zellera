package zel.protocol.generated

data object GetCollectibleMintableItemTypesIn

object GetCollectibleMintableItemTypesInCodec : IncomingCodec<GetCollectibleMintableItemTypesIn> {
    override val name = "GetCollectibleMintableItemTypesIn"

    override fun read(r: PacketReader): GetCollectibleMintableItemTypesIn = GetCollectibleMintableItemTypesIn

    override fun write(w: PacketWriter, m: GetCollectibleMintableItemTypesIn) = writeGetCollectibleMintableItemTypesIn(w, m)

    @Suppress("UNUSED_PARAMETER")
    private fun writeGetCollectibleMintableItemTypesIn(w: PacketWriter, m: GetCollectibleMintableItemTypesIn) {
    }
}
