package zel.protocol.generated

data class JoinGroupIn(
    val guildId: Int,
)

object JoinGroupInCodec : IncomingCodec<JoinGroupIn> {
    override val name = "JoinGroupIn"

    override fun read(r: PacketReader): JoinGroupIn = readJoinGroupIn(r)

    override fun write(w: PacketWriter, m: JoinGroupIn) = writeJoinGroupIn(w, m)

    private fun readJoinGroupIn(r: PacketReader): JoinGroupIn {
        val vGuildId = r.int()
        return JoinGroupIn(vGuildId)
    }

    private fun writeJoinGroupIn(w: PacketWriter, m: JoinGroupIn) {
        w.int(m.guildId)
    }
}
