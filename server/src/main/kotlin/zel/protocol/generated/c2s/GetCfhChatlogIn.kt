package zel.protocol.generated

data class GetCfhChatlogIn(
    val int1: Int,
)

object GetCfhChatlogInCodec : IncomingCodec<GetCfhChatlogIn> {
    override val name = "GetCfhChatlogIn"

    override fun read(r: PacketReader): GetCfhChatlogIn = readGetCfhChatlogIn(r)

    override fun write(w: PacketWriter, m: GetCfhChatlogIn) = writeGetCfhChatlogIn(w, m)

    private fun readGetCfhChatlogIn(r: PacketReader): GetCfhChatlogIn {
        val vInt1 = r.int()
        return GetCfhChatlogIn(vInt1)
    }

    private fun writeGetCfhChatlogIn(w: PacketWriter, m: GetCfhChatlogIn) {
        w.int(m.int1)
    }
}
