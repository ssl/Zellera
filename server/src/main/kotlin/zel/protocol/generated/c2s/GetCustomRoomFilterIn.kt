package zel.protocol.generated

data class GetCustomRoomFilterIn(
    val roomId: Int,
)

object GetCustomRoomFilterInCodec : IncomingCodec<GetCustomRoomFilterIn> {
    override val name = "GetCustomRoomFilterIn"

    override fun read(r: PacketReader): GetCustomRoomFilterIn = readGetCustomRoomFilterIn(r)

    override fun write(w: PacketWriter, m: GetCustomRoomFilterIn) = writeGetCustomRoomFilterIn(w, m)

    private fun readGetCustomRoomFilterIn(r: PacketReader): GetCustomRoomFilterIn {
        val vRoomId = r.int()
        return GetCustomRoomFilterIn(vRoomId)
    }

    private fun writeGetCustomRoomFilterIn(w: PacketWriter, m: GetCustomRoomFilterIn) {
        w.int(m.roomId)
    }
}
