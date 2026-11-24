package zel.protocol.generated

data object GetGiftIn

object GetGiftInCodec : IncomingCodec<GetGiftIn> {
    override val name = "GetGiftIn"

    override fun read(r: PacketReader): GetGiftIn = GetGiftIn

    override fun write(w: PacketWriter, m: GetGiftIn) = writeGetGiftIn(w, m)

    @Suppress("UNUSED_PARAMETER")
    private fun writeGetGiftIn(w: PacketWriter, m: GetGiftIn) {
    }
}
