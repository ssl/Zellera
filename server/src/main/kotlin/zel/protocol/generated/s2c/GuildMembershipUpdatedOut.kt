package zel.protocol.generated

data class GuildMembershipUpdatedOut(
    val int1: Int,
    val int2: Int,
    val int3: Int,
    val string1: String,
    val string2: String,
    val string3: String,
)

object GuildMembershipUpdatedOutCodec : OutgoingCodec<GuildMembershipUpdatedOut> {
    override val name = "GuildMembershipUpdatedOut"

    override fun read(r: PacketReader): GuildMembershipUpdatedOut = readGuildMembershipUpdatedOut(r)

    override fun write(w: PacketWriter, m: GuildMembershipUpdatedOut) = writeGuildMembershipUpdatedOut(w, m)

    private fun readGuildMembershipUpdatedOut(r: PacketReader): GuildMembershipUpdatedOut {
        val vInt1 = r.int()
        val vInt2 = r.int()
        val vInt3 = r.int()
        val vString1 = r.string()
        val vString2 = r.string()
        val vString3 = r.string()
        return GuildMembershipUpdatedOut(vInt1, vInt2, vInt3, vString1, vString2, vString3)
    }

    private fun writeGuildMembershipUpdatedOut(w: PacketWriter, m: GuildMembershipUpdatedOut) {
        w.int(m.int1)
        w.int(m.int2)
        w.int(m.int3)
        w.string(m.string1)
        w.string(m.string2)
        w.string(m.string3)
    }
}
