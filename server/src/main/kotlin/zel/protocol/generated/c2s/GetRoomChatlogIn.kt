package zel.protocol.generated

data class GetRoomChatlogIn(
    val int1: Int,
    val int2: Int,
)

object GetRoomChatlogInCodec : IncomingCodec<GetRoomChatlogIn> {
    override val name = "GetRoomChatlogIn"

    override fun read(r: PacketReader): GetRoomChatlogIn = readGetRoomChatlogIn(r)

    override fun write(w: PacketWriter, m: GetRoomChatlogIn) = writeGetRoomChatlogIn(w, m)

    private fun readGetRoomChatlogIn(r: PacketReader): GetRoomChatlogIn {
        val vInt1 = r.int()
        val vInt2 = r.int()
        return GetRoomChatlogIn(vInt1, vInt2)
    }

    private fun writeGetRoomChatlogIn(w: PacketWriter, m: GetRoomChatlogIn) {
        w.int(m.int1)
        w.int(m.int2)
    }
}
