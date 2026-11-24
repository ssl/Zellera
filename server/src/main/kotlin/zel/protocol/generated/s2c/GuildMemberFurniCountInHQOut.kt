package zel.protocol.generated

data class GuildMemberFurniCountInHQOut(
    val int1: Int,
    val int2: Int,
)

object GuildMemberFurniCountInHQOutCodec : OutgoingCodec<GuildMemberFurniCountInHQOut> {
    override val name = "GuildMemberFurniCountInHQOut"

    override fun read(r: PacketReader): GuildMemberFurniCountInHQOut = readGuildMemberFurniCountInHQOut(r)

    override fun write(w: PacketWriter, m: GuildMemberFurniCountInHQOut) = writeGuildMemberFurniCountInHQOut(w, m)

    private fun readGuildMemberFurniCountInHQOut(r: PacketReader): GuildMemberFurniCountInHQOut {
        val vInt1 = r.int()
        val vInt2 = r.int()
        return GuildMemberFurniCountInHQOut(vInt1, vInt2)
    }

    private fun writeGuildMemberFurniCountInHQOut(w: PacketWriter, m: GuildMemberFurniCountInHQOut) {
        w.int(m.int1)
        w.int(m.int2)
    }
}
