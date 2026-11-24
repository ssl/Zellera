package zel.protocol.generated

data class UnbanPlayerFromRoomIn(
    val userId: Int,
    val roomId: Int,
)

object UnbanPlayerFromRoomInCodec : IncomingCodec<UnbanPlayerFromRoomIn> {
    override val name = "UnbanPlayerFromRoomIn"

    override fun read(r: PacketReader): UnbanPlayerFromRoomIn = readUnbanPlayerFromRoomIn(r)

    override fun write(w: PacketWriter, m: UnbanPlayerFromRoomIn) = writeUnbanPlayerFromRoomIn(w, m)

    private fun readUnbanPlayerFromRoomIn(r: PacketReader): UnbanPlayerFromRoomIn {
        val vUserId = r.int()
        val vRoomId = r.int()
        return UnbanPlayerFromRoomIn(vUserId, vRoomId)
    }

    private fun writeUnbanPlayerFromRoomIn(w: PacketWriter, m: UnbanPlayerFromRoomIn) {
        w.int(m.userId)
        w.int(m.roomId)
    }
}
