package zel.protocol.generated

data class ChatIn(
    val string1: String,
    val int1: Int,
    val int2: Int,
)

object ChatInCodec : IncomingCodec<ChatIn> {
    override val name = "ChatIn"

    override fun read(r: PacketReader): ChatIn = readChatIn(r)

    override fun write(w: PacketWriter, m: ChatIn) = writeChatIn(w, m)

    private fun readChatIn(r: PacketReader): ChatIn {
        val vString1 = r.string()
        val vInt1 = r.int()
        val vInt2 = r.int()
        return ChatIn(vString1, vInt1, vInt2)
    }

    private fun writeChatIn(w: PacketWriter, m: ChatIn) {
        w.string(m.string1)
        w.int(m.int1)
        w.int(m.int2)
    }
}
