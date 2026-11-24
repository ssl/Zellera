package zel.protocol.generated

data class GetSelectedBadgesIn(
    val userId: Int,
)

object GetSelectedBadgesInCodec : IncomingCodec<GetSelectedBadgesIn> {
    override val name = "GetSelectedBadgesIn"

    override fun read(r: PacketReader): GetSelectedBadgesIn = readGetSelectedBadgesIn(r)

    override fun write(w: PacketWriter, m: GetSelectedBadgesIn) = writeGetSelectedBadgesIn(w, m)

    private fun readGetSelectedBadgesIn(r: PacketReader): GetSelectedBadgesIn {
        val vUserId = r.int()
        return GetSelectedBadgesIn(vUserId)
    }

    private fun writeGetSelectedBadgesIn(w: PacketWriter, m: GetSelectedBadgesIn) {
        w.int(m.userId)
    }
}
