package zel.protocol.generated

data class GetMessengerHistoryIn(
    val chatId: Int,
    val before: String,
)

object GetMessengerHistoryInCodec : IncomingCodec<GetMessengerHistoryIn> {
    override val name = "GetMessengerHistoryIn"

    override fun read(r: PacketReader): GetMessengerHistoryIn = readGetMessengerHistoryIn(r)

    override fun write(w: PacketWriter, m: GetMessengerHistoryIn) = writeGetMessengerHistoryIn(w, m)

    private fun readGetMessengerHistoryIn(r: PacketReader): GetMessengerHistoryIn {
        val vChatId = r.int()
        val vBefore = r.string()
        return GetMessengerHistoryIn(vChatId, vBefore)
    }

    private fun writeGetMessengerHistoryIn(w: PacketWriter, m: GetMessengerHistoryIn) {
        w.int(m.chatId)
        w.string(m.before)
    }
}
