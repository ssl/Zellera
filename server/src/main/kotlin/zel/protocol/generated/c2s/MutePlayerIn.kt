package zel.protocol.generated

data class MutePlayerIn(
    val userId: Int,
    val roomId: Int,
    val minutes: Int,
)

object MutePlayerInCodec : IncomingCodec<MutePlayerIn> {
    override val name = "MutePlayerIn"

    override fun read(r: PacketReader): MutePlayerIn = readMutePlayerIn(r)

    override fun write(w: PacketWriter, m: MutePlayerIn) = writeMutePlayerIn(w, m)

    private fun readMutePlayerIn(r: PacketReader): MutePlayerIn {
        val vUserId = r.int()
        val vRoomId = r.int()
        val vMinutes = r.int()
        return MutePlayerIn(vUserId, vRoomId, vMinutes)
    }

    private fun writeMutePlayerIn(w: PacketWriter, m: MutePlayerIn) {
        w.int(m.userId)
        w.int(m.roomId)
        w.int(m.minutes)
    }
}
