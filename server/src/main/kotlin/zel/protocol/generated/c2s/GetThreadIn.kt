package zel.protocol.generated

data class GetThreadIn(
    val guildId: Int,
    val threadId: Int,
)

object GetThreadInCodec : IncomingCodec<GetThreadIn> {
    override val name = "GetThreadIn"

    override fun read(r: PacketReader): GetThreadIn = readGetThreadIn(r)

    override fun write(w: PacketWriter, m: GetThreadIn) = writeGetThreadIn(w, m)

    private fun readGetThreadIn(r: PacketReader): GetThreadIn {
        val vGuildId = r.int()
        val vThreadId = r.int()
        return GetThreadIn(vGuildId, vThreadId)
    }

    private fun writeGetThreadIn(w: PacketWriter, m: GetThreadIn) {
        w.int(m.guildId)
        w.int(m.threadId)
    }
}
