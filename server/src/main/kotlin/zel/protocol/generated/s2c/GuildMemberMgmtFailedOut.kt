package zel.protocol.generated

data class GuildMemberMgmtFailedOut(
    val int1: Int,
    val int2: Int,
)

object GuildMemberMgmtFailedOutCodec : OutgoingCodec<GuildMemberMgmtFailedOut> {
    override val name = "GuildMemberMgmtFailedOut"

    override fun read(r: PacketReader): GuildMemberMgmtFailedOut = readGuildMemberMgmtFailedOut(r)

    override fun write(w: PacketWriter, m: GuildMemberMgmtFailedOut) = writeGuildMemberMgmtFailedOut(w, m)

    private fun readGuildMemberMgmtFailedOut(r: PacketReader): GuildMemberMgmtFailedOut {
        val vInt1 = r.int()
        val vInt2 = r.int()
        return GuildMemberMgmtFailedOut(vInt1, vInt2)
    }

    private fun writeGuildMemberMgmtFailedOut(w: PacketWriter, m: GuildMemberMgmtFailedOut) {
        w.int(m.int1)
        w.int(m.int2)
    }
}
