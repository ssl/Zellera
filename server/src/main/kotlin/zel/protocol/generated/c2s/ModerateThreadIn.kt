package zel.protocol.generated

data class ModerateThreadIn(
    val guildId: Int,
    val threadId: Int,
    val state: Int,
)

object ModerateThreadInCodec : IncomingCodec<ModerateThreadIn> {
    override val name = "ModerateThreadIn"

    override fun read(r: PacketReader): ModerateThreadIn = readModerateThreadIn(r)

    override fun write(w: PacketWriter, m: ModerateThreadIn) = writeModerateThreadIn(w, m)

    private fun readModerateThreadIn(r: PacketReader): ModerateThreadIn {
        val vGuildId = r.int()
        val vThreadId = r.int()
        val vState = r.int()
        return ModerateThreadIn(vGuildId, vThreadId, vState)
    }

    private fun writeModerateThreadIn(w: PacketWriter, m: ModerateThreadIn) {
        w.int(m.guildId)
        w.int(m.threadId)
        w.int(m.state)
    }
}
