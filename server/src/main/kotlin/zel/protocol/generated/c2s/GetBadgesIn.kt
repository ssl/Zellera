package zel.protocol.generated

data object GetBadgesIn

object GetBadgesInCodec : IncomingCodec<GetBadgesIn> {
    override val name = "GetBadgesIn"

    override fun read(r: PacketReader): GetBadgesIn = GetBadgesIn

    override fun write(w: PacketWriter, m: GetBadgesIn) = writeGetBadgesIn(w, m)

    @Suppress("UNUSED_PARAMETER")
    private fun writeGetBadgesIn(w: PacketWriter, m: GetBadgesIn) {
    }
}
