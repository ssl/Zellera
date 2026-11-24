package zel.protocol.generated

data object GetSelectedNftWardrobeOutfitIn

object GetSelectedNftWardrobeOutfitInCodec : IncomingCodec<GetSelectedNftWardrobeOutfitIn> {
    override val name = "GetSelectedNftWardrobeOutfitIn"

    override fun read(r: PacketReader): GetSelectedNftWardrobeOutfitIn = GetSelectedNftWardrobeOutfitIn

    override fun write(w: PacketWriter, m: GetSelectedNftWardrobeOutfitIn) = writeGetSelectedNftWardrobeOutfitIn(w, m)

    @Suppress("UNUSED_PARAMETER")
    private fun writeGetSelectedNftWardrobeOutfitIn(w: PacketWriter, m: GetSelectedNftWardrobeOutfitIn) {
    }
}
