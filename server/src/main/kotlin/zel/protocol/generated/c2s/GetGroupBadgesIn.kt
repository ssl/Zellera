package zel.protocol.generated

data object GetGroupBadgesIn

object GetGroupBadgesInCodec : IncomingCodec<GetGroupBadgesIn> {
    override val name = "GetGroupBadgesIn"

    override fun read(r: PacketReader): GetGroupBadgesIn = GetGroupBadgesIn

    override fun write(w: PacketWriter, m: GetGroupBadgesIn) = writeGetGroupBadgesIn(w, m)

    @Suppress("UNUSED_PARAMETER")
    private fun writeGetGroupBadgesIn(w: PacketWriter, m: GetGroupBadgesIn) {
    }
}
