package zel.protocol.generated

data class GetForumStatsIn(
    val guildId: Int,
)

object GetForumStatsInCodec : IncomingCodec<GetForumStatsIn> {
    override val name = "GetForumStatsIn"

    override fun read(r: PacketReader): GetForumStatsIn = readGetForumStatsIn(r)

    override fun write(w: PacketWriter, m: GetForumStatsIn) = writeGetForumStatsIn(w, m)

    private fun readGetForumStatsIn(r: PacketReader): GetForumStatsIn {
        val vGuildId = r.int()
        return GetForumStatsIn(vGuildId)
    }

    private fun writeGetForumStatsIn(w: PacketWriter, m: GetForumStatsIn) {
        w.int(m.guildId)
    }
}
