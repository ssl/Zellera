package zel.protocol.generated

data class RoomSettingsSaveErrorOut(
    val int1: Int,
    val int2: Int,
    val string1: String,
)

object RoomSettingsSaveErrorOutCodec : OutgoingCodec<RoomSettingsSaveErrorOut> {
    override val name = "RoomSettingsSaveErrorOut"

    override fun read(r: PacketReader): RoomSettingsSaveErrorOut = readRoomSettingsSaveErrorOut(r)

    override fun write(w: PacketWriter, m: RoomSettingsSaveErrorOut) = writeRoomSettingsSaveErrorOut(w, m)

    private fun readRoomSettingsSaveErrorOut(r: PacketReader): RoomSettingsSaveErrorOut {
        val vInt1 = r.int()
        val vInt2 = r.int()
        val vString1 = r.string()
        return RoomSettingsSaveErrorOut(vInt1, vInt2, vString1)
    }

    private fun writeRoomSettingsSaveErrorOut(w: PacketWriter, m: RoomSettingsSaveErrorOut) {
        w.int(m.int1)
        w.int(m.int2)
        w.string(m.string1)
    }
}
