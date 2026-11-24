package zel.protocol.generated

data class GetModeratorRoomInfoIn(
    val int1: Int,
)

object GetModeratorRoomInfoInCodec : IncomingCodec<GetModeratorRoomInfoIn> {
    override val name = "GetModeratorRoomInfoIn"

    override fun read(r: PacketReader): GetModeratorRoomInfoIn = readGetModeratorRoomInfoIn(r)

    override fun write(w: PacketWriter, m: GetModeratorRoomInfoIn) = writeGetModeratorRoomInfoIn(w, m)

    private fun readGetModeratorRoomInfoIn(r: PacketReader): GetModeratorRoomInfoIn {
        val vInt1 = r.int()
        return GetModeratorRoomInfoIn(vInt1)
    }

    private fun writeGetModeratorRoomInfoIn(w: PacketWriter, m: GetModeratorRoomInfoIn) {
        w.int(m.int1)
    }
}
