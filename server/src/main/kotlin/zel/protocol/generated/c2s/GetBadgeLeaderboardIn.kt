package zel.protocol.generated

data class GetBadgeLeaderboardIn(
    val type: Int,
    val rarity: Int,
    val page: Int,
    val size: Int,
)

object GetBadgeLeaderboardInCodec : IncomingCodec<GetBadgeLeaderboardIn> {
    override val name = "GetBadgeLeaderboardIn"

    override fun read(r: PacketReader): GetBadgeLeaderboardIn = readGetBadgeLeaderboardIn(r)

    override fun write(w: PacketWriter, m: GetBadgeLeaderboardIn) = writeGetBadgeLeaderboardIn(w, m)

    private fun readGetBadgeLeaderboardIn(r: PacketReader): GetBadgeLeaderboardIn {
        val vType = r.int()
        val vRarity = r.int()
        val vPage = r.int()
        val vSize = r.int()
        return GetBadgeLeaderboardIn(vType, vRarity, vPage, vSize)
    }

    private fun writeGetBadgeLeaderboardIn(w: PacketWriter, m: GetBadgeLeaderboardIn) {
        w.int(m.type)
        w.int(m.rarity)
        w.int(m.page)
        w.int(m.size)
    }
}
