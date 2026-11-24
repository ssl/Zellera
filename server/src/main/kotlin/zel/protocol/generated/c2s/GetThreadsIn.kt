package zel.protocol.generated

data class GetThreadsIn(
    val guildId: Int,
    val index: Int,
    val int1: Int,
)

object GetThreadsInCodec : IncomingCodec<GetThreadsIn> {
    override val name = "GetThreadsIn"

    override fun read(r: PacketReader): GetThreadsIn = readGetThreadsIn(r)

    override fun write(w: PacketWriter, m: GetThreadsIn) = writeGetThreadsIn(w, m)

    private fun readGetThreadsIn(r: PacketReader): GetThreadsIn {
        val vGuildId = r.int()
        val vIndex = r.int()
        val vInt1 = r.int()
        return GetThreadsIn(vGuildId, vIndex, vInt1)
    }

    private fun writeGetThreadsIn(w: PacketWriter, m: GetThreadsIn) {
        w.int(m.guildId)
        w.int(m.index)
        w.int(m.int1)
    }
}
