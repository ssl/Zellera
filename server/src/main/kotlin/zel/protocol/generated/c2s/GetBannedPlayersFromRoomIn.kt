package zel.protocol.generated

data class GetBannedPlayersFromRoomIn(
    val roomId: Int,
)

object GetBannedPlayersFromRoomInCodec : IncomingCodec<GetBannedPlayersFromRoomIn> {
    override val name = "GetBannedPlayersFromRoomIn"

    override fun read(r: PacketReader): GetBannedPlayersFromRoomIn = readGetBannedPlayersFromRoomIn(r)

    override fun write(w: PacketWriter, m: GetBannedPlayersFromRoomIn) = writeGetBannedPlayersFromRoomIn(w, m)

    private fun readGetBannedPlayersFromRoomIn(r: PacketReader): GetBannedPlayersFromRoomIn {
        val vRoomId = r.int()
        return GetBannedPlayersFromRoomIn(vRoomId)
    }

    private fun writeGetBannedPlayersFromRoomIn(w: PacketWriter, m: GetBannedPlayersFromRoomIn) {
        w.int(m.roomId)
    }
}
