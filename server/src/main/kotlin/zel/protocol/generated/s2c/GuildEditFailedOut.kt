package zel.protocol.generated

data class GuildEditFailedOut(
    val int1: Int,
)

object GuildEditFailedOutCodec : OutgoingCodec<GuildEditFailedOut> {
    override val name = "GuildEditFailedOut"

    override fun read(r: PacketReader): GuildEditFailedOut = readGuildEditFailedOut(r)

    override fun write(w: PacketWriter, m: GuildEditFailedOut) = writeGuildEditFailedOut(w, m)

    private fun readGuildEditFailedOut(r: PacketReader): GuildEditFailedOut {
        val vInt1 = r.int()
        return GuildEditFailedOut(vInt1)
    }

    private fun writeGuildEditFailedOut(w: PacketWriter, m: GuildEditFailedOut) {
        w.int(m.int1)
    }
}
