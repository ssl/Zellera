package zel.protocol.generated

data class UpdateHomeRoomIn(
    val roomId: Int,
)

object UpdateHomeRoomInCodec : IncomingCodec<UpdateHomeRoomIn> {
    override val name = "UpdateHomeRoomIn"

    override fun read(r: PacketReader): UpdateHomeRoomIn = readUpdateHomeRoomIn(r)

    override fun write(w: PacketWriter, m: UpdateHomeRoomIn) = writeUpdateHomeRoomIn(w, m)

    private fun readUpdateHomeRoomIn(r: PacketReader): UpdateHomeRoomIn {
        val vRoomId = r.int()
        return UpdateHomeRoomIn(vRoomId)
    }

    private fun writeUpdateHomeRoomIn(w: PacketWriter, m: UpdateHomeRoomIn) {
        w.int(m.roomId)
    }
}
