package zel.protocol.generated

data object GetClubGiftIn

object GetClubGiftInCodec : IncomingCodec<GetClubGiftIn> {
    override val name = "GetClubGiftIn"

    override fun read(r: PacketReader): GetClubGiftIn = GetClubGiftIn

    override fun write(w: PacketWriter, m: GetClubGiftIn) = writeGetClubGiftIn(w, m)

    @Suppress("UNUSED_PARAMETER")
    private fun writeGetClubGiftIn(w: PacketWriter, m: GetClubGiftIn) {
    }
}
