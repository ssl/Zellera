package zel.protocol.generated

data class GetGuestRoomIn(
    val roomId: Int,
    val isLoading: Int,
    val checkEntry: Int,
)

object GetGuestRoomInCodec : IncomingCodec<GetGuestRoomIn> {
    override val name = "GetGuestRoomIn"

    override fun read(r: PacketReader): GetGuestRoomIn = readGetGuestRoomIn(r)

    override fun write(w: PacketWriter, m: GetGuestRoomIn) = writeGetGuestRoomIn(w, m)

    private fun readGetGuestRoomIn(r: PacketReader): GetGuestRoomIn {
        val vRoomId = r.int()
        val vIsLoading = r.int()
        val vCheckEntry = r.int()
        return GetGuestRoomIn(vRoomId, vIsLoading, vCheckEntry)
    }

    private fun writeGetGuestRoomIn(w: PacketWriter, m: GetGuestRoomIn) {
        w.int(m.roomId)
        w.int(m.isLoading)
        w.int(m.checkEntry)
    }
}
