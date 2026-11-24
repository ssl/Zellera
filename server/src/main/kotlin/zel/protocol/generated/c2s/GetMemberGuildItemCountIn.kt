package zel.protocol.generated

data class GetMemberGuildItemCountIn(
    val guildId: Int,
    val userId: Int,
)

object GetMemberGuildItemCountInCodec : IncomingCodec<GetMemberGuildItemCountIn> {
    override val name = "GetMemberGuildItemCountIn"

    override fun read(r: PacketReader): GetMemberGuildItemCountIn = readGetMemberGuildItemCountIn(r)

    override fun write(w: PacketWriter, m: GetMemberGuildItemCountIn) = writeGetMemberGuildItemCountIn(w, m)

    private fun readGetMemberGuildItemCountIn(r: PacketReader): GetMemberGuildItemCountIn {
        val vGuildId = r.int()
        val vUserId = r.int()
        return GetMemberGuildItemCountIn(vGuildId, vUserId)
    }

    private fun writeGetMemberGuildItemCountIn(w: PacketWriter, m: GetMemberGuildItemCountIn) {
        w.int(m.guildId)
        w.int(m.userId)
    }
}
