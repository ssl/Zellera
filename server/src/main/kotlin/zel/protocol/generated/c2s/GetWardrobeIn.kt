package zel.protocol.generated

data object GetWardrobeIn

object GetWardrobeInCodec : IncomingCodec<GetWardrobeIn> {
    override val name = "GetWardrobeIn"

    override fun read(r: PacketReader): GetWardrobeIn = GetWardrobeIn

    override fun write(w: PacketWriter, m: GetWardrobeIn) = writeGetWardrobeIn(w, m)

    @Suppress("UNUSED_PARAMETER")
    private fun writeGetWardrobeIn(w: PacketWriter, m: GetWardrobeIn) {
    }
}
