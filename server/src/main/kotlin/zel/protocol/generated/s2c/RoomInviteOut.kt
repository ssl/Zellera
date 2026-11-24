package zel.protocol.generated

data class RoomInviteOut(
    val int1: Int,
    val string1: String,
)

object RoomInviteOutCodec : OutgoingCodec<RoomInviteOut> {
    override val name = "RoomInviteOut"

    override fun read(r: PacketReader): RoomInviteOut = readRoomInviteOut(r)

    override fun write(w: PacketWriter, m: RoomInviteOut) = writeRoomInviteOut(w, m)

    private fun readRoomInviteOut(r: PacketReader): RoomInviteOut {
        val vInt1 = r.int()
        val vString1 = r.string()
        return RoomInviteOut(vInt1, vString1)
    }

    private fun writeRoomInviteOut(w: PacketWriter, m: RoomInviteOut) {
        w.int(m.int1)
        w.string(m.string1)
    }
}
