package zel.protocol.generated

data class PlayerUnbannedFromRoomOut(
    val int1: Int,
    val int2: Int,
)

object PlayerUnbannedFromRoomOutCodec : OutgoingCodec<PlayerUnbannedFromRoomOut> {
    override val name = "PlayerUnbannedFromRoomOut"

    override fun read(r: PacketReader): PlayerUnbannedFromRoomOut = readPlayerUnbannedFromRoomOut(r)

    override fun write(w: PacketWriter, m: PlayerUnbannedFromRoomOut) = writePlayerUnbannedFromRoomOut(w, m)

    private fun readPlayerUnbannedFromRoomOut(r: PacketReader): PlayerUnbannedFromRoomOut {
        val vInt1 = r.int()
        val vInt2 = r.int()
        return PlayerUnbannedFromRoomOut(vInt1, vInt2)
    }

    private fun writePlayerUnbannedFromRoomOut(w: PacketWriter, m: PlayerUnbannedFromRoomOut) {
        w.int(m.int1)
        w.int(m.int2)
    }
}
