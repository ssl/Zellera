package zel.protocol.generated

data class MuteAllInRoomOut(
    val boolean1: Boolean,
)

object MuteAllInRoomOutCodec : OutgoingCodec<MuteAllInRoomOut> {
    override val name = "MuteAllInRoomOut"

    override fun read(r: PacketReader): MuteAllInRoomOut = readMuteAllInRoomOut(r)

    override fun write(w: PacketWriter, m: MuteAllInRoomOut) = writeMuteAllInRoomOut(w, m)

    private fun readMuteAllInRoomOut(r: PacketReader): MuteAllInRoomOut {
        val vBoolean1 = r.boolean()
        return MuteAllInRoomOut(vBoolean1)
    }

    private fun writeMuteAllInRoomOut(w: PacketWriter, m: MuteAllInRoomOut) {
        w.boolean(m.boolean1)
    }
}
