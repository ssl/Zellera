package zel.protocol.generated

data class GuildBaseSearchIn(
    val adIndex: Int,
)

object GuildBaseSearchInCodec : IncomingCodec<GuildBaseSearchIn> {
    override val name = "GuildBaseSearchIn"

    override fun read(r: PacketReader): GuildBaseSearchIn = readGuildBaseSearchIn(r)

    override fun write(w: PacketWriter, m: GuildBaseSearchIn) = writeGuildBaseSearchIn(w, m)

    private fun readGuildBaseSearchIn(r: PacketReader): GuildBaseSearchIn {
        val vAdIndex = r.int()
        return GuildBaseSearchIn(vAdIndex)
    }

    private fun writeGuildBaseSearchIn(w: PacketWriter, m: GuildBaseSearchIn) {
        w.int(m.adIndex)
    }
}
