package zel.protocol.generated

data class GetBadgeInfoIn(
    val badgeCode: String,
)

object GetBadgeInfoInCodec : IncomingCodec<GetBadgeInfoIn> {
    override val name = "GetBadgeInfoIn"

    override fun read(r: PacketReader): GetBadgeInfoIn = readGetBadgeInfoIn(r)

    override fun write(w: PacketWriter, m: GetBadgeInfoIn) = writeGetBadgeInfoIn(w, m)

    private fun readGetBadgeInfoIn(r: PacketReader): GetBadgeInfoIn {
        val vBadgeCode = r.string()
        return GetBadgeInfoIn(vBadgeCode)
    }

    private fun writeGetBadgeInfoIn(w: PacketWriter, m: GetBadgeInfoIn) {
        w.string(m.badgeCode)
    }
}
