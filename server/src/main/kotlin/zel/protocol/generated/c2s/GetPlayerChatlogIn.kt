package zel.protocol.generated

data class GetPlayerChatlogIn(
    val int1: Int,
)

object GetPlayerChatlogInCodec : IncomingCodec<GetPlayerChatlogIn> {
    override val name = "GetPlayerChatlogIn"

    override fun read(r: PacketReader): GetPlayerChatlogIn = readGetPlayerChatlogIn(r)

    override fun write(w: PacketWriter, m: GetPlayerChatlogIn) = writeGetPlayerChatlogIn(w, m)

    private fun readGetPlayerChatlogIn(r: PacketReader): GetPlayerChatlogIn {
        val vInt1 = r.int()
        return GetPlayerChatlogIn(vInt1)
    }

    private fun writeGetPlayerChatlogIn(w: PacketWriter, m: GetPlayerChatlogIn) {
        w.int(m.int1)
    }
}
