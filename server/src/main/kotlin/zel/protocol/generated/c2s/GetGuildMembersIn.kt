package zel.protocol.generated

data class GetGuildMembersIn(
    val groupId: Int,
    val pageId: Int,
    val query: String,
    val levelId: Int,
)

object GetGuildMembersInCodec : IncomingCodec<GetGuildMembersIn> {
    override val name = "GetGuildMembersIn"

    override fun read(r: PacketReader): GetGuildMembersIn = readGetGuildMembersIn(r)

    override fun write(w: PacketWriter, m: GetGuildMembersIn) = writeGetGuildMembersIn(w, m)

    private fun readGetGuildMembersIn(r: PacketReader): GetGuildMembersIn {
        val vGroupId = r.int()
        val vPageId = r.int()
        val vQuery = r.string()
        val vLevelId = r.int()
        return GetGuildMembersIn(vGroupId, vPageId, vQuery, vLevelId)
    }

    private fun writeGetGuildMembersIn(w: PacketWriter, m: GetGuildMembersIn) {
        w.int(m.groupId)
        w.int(m.pageId)
        w.string(m.query)
        w.int(m.levelId)
    }
}
