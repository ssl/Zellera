package zel.protocol.generated

data class GetRoomVisitsIn(
    val int1: Int,
)

object GetRoomVisitsInCodec : IncomingCodec<GetRoomVisitsIn> {
    override val name = "GetRoomVisitsIn"

    override fun read(r: PacketReader): GetRoomVisitsIn = readGetRoomVisitsIn(r)

    override fun write(w: PacketWriter, m: GetRoomVisitsIn) = writeGetRoomVisitsIn(w, m)

    private fun readGetRoomVisitsIn(r: PacketReader): GetRoomVisitsIn {
        val vInt1 = r.int()
        return GetRoomVisitsIn(vInt1)
    }

    private fun writeGetRoomVisitsIn(w: PacketWriter, m: GetRoomVisitsIn) {
        w.int(m.int1)
    }
}
