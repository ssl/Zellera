package zel.protocol.generated

data object GetPlayerNftWardrobeIn

object GetPlayerNftWardrobeInCodec : IncomingCodec<GetPlayerNftWardrobeIn> {
    override val name = "GetPlayerNftWardrobeIn"

    override fun read(r: PacketReader): GetPlayerNftWardrobeIn = GetPlayerNftWardrobeIn

    override fun write(w: PacketWriter, m: GetPlayerNftWardrobeIn) = writeGetPlayerNftWardrobeIn(w, m)

    @Suppress("UNUSED_PARAMETER")
    private fun writeGetPlayerNftWardrobeIn(w: PacketWriter, m: GetPlayerNftWardrobeIn) {
    }
}
