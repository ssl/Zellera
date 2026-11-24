package zel.protocol.generated

data class GuildMembershipRejectedOut(
    val int1: Int,
    val int2: Int,
)

object GuildMembershipRejectedOutCodec : OutgoingCodec<GuildMembershipRejectedOut> {
    override val name = "GuildMembershipRejectedOut"

    override fun read(r: PacketReader): GuildMembershipRejectedOut = readGuildMembershipRejectedOut(r)

    override fun write(w: PacketWriter, m: GuildMembershipRejectedOut) = writeGuildMembershipRejectedOut(w, m)

    private fun readGuildMembershipRejectedOut(r: PacketReader): GuildMembershipRejectedOut {
        val vInt1 = r.int()
        val vInt2 = r.int()
        return GuildMembershipRejectedOut(vInt1, vInt2)
    }

    private fun writeGuildMembershipRejectedOut(w: PacketWriter, m: GuildMembershipRejectedOut) {
        w.int(m.int1)
        w.int(m.int2)
    }
}
