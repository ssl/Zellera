package zel.protocol.generated

data class RoomSettingsErrorOut(
    val int1: Int,
    val int2: Int,
)

object RoomSettingsErrorOutCodec : OutgoingCodec<RoomSettingsErrorOut> {
    override val name = "RoomSettingsErrorOut"

    override fun read(r: PacketReader): RoomSettingsErrorOut = readRoomSettingsErrorOut(r)

    override fun write(w: PacketWriter, m: RoomSettingsErrorOut) = writeRoomSettingsErrorOut(w, m)

    private fun readRoomSettingsErrorOut(r: PacketReader): RoomSettingsErrorOut {
        val vInt1 = r.int()
        val vInt2 = r.int()
        return RoomSettingsErrorOut(vInt1, vInt2)
    }

    private fun writeRoomSettingsErrorOut(w: PacketWriter, m: RoomSettingsErrorOut) {
        w.int(m.int1)
        w.int(m.int2)
    }
}
