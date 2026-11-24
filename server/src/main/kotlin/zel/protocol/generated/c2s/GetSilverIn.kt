package zel.protocol.generated

data object GetSilverIn

object GetSilverInCodec : IncomingCodec<GetSilverIn> {
    override val name = "GetSilverIn"

    override fun read(r: PacketReader): GetSilverIn = GetSilverIn

    override fun write(w: PacketWriter, m: GetSilverIn) = writeGetSilverIn(w, m)

    @Suppress("UNUSED_PARAMETER")
    private fun writeGetSilverIn(w: PacketWriter, m: GetSilverIn) {
    }
}
