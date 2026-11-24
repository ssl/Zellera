package zel.protocol.generated

data class GetMessagesIn(
    val guildId: Int,
    val threadId: Int,
    val index: Int,
    val limit: Int,
)

object GetMessagesInCodec : IncomingCodec<GetMessagesIn> {
    override val name = "GetMessagesIn"

    override fun read(r: PacketReader): GetMessagesIn = readGetMessagesIn(r)

    override fun write(w: PacketWriter, m: GetMessagesIn) = writeGetMessagesIn(w, m)

    private fun readGetMessagesIn(r: PacketReader): GetMessagesIn {
        val vGuildId = r.int()
        val vThreadId = r.int()
        val vIndex = r.int()
        val vLimit = r.int()
        return GetMessagesIn(vGuildId, vThreadId, vIndex, vLimit)
    }

    private fun writeGetMessagesIn(w: PacketWriter, m: GetMessagesIn) {
        w.int(m.guildId)
        w.int(m.threadId)
        w.int(m.index)
        w.int(m.limit)
    }
}
