package zel.protocol.generated

data class RoomSettingsSavedOut(
    val int1: Int,
)

object RoomSettingsSavedOutCodec : OutgoingCodec<RoomSettingsSavedOut> {
    override val name = "RoomSettingsSavedOut"

    override fun read(r: PacketReader): RoomSettingsSavedOut = readRoomSettingsSavedOut(r)

    override fun write(w: PacketWriter, m: RoomSettingsSavedOut) = writeRoomSettingsSavedOut(w, m)

    private fun readRoomSettingsSavedOut(r: PacketReader): RoomSettingsSavedOut {
        val vInt1 = r.int()
        return RoomSettingsSavedOut(vInt1)
    }

    private fun writeRoomSettingsSavedOut(w: PacketWriter, m: RoomSettingsSavedOut) {
        w.int(m.int1)
    }
}
