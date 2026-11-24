package zel.protocol.generated

data class GuildCreatedOut(
    val int1: Int,
    val int2: Int,
)

object GuildCreatedOutCodec : OutgoingCodec<GuildCreatedOut> {
    override val name = "GuildCreatedOut"

    override fun read(r: PacketReader): GuildCreatedOut = readGuildCreatedOut(r)

    override fun write(w: PacketWriter, m: GuildCreatedOut) = writeGuildCreatedOut(w, m)

    private fun readGuildCreatedOut(r: PacketReader): GuildCreatedOut {
        val vInt1 = r.int()
        val vInt2 = r.int()
        return GuildCreatedOut(vInt1, vInt2)
    }

    private fun writeGuildCreatedOut(w: PacketWriter, m: GuildCreatedOut) {
        w.int(m.int1)
        w.int(m.int2)
    }
}
