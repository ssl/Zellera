package zel.protocol.generated

data class SetRoomCameraPreferencesIn(
    val blockFollow: Boolean,
)

object SetRoomCameraPreferencesInCodec : IncomingCodec<SetRoomCameraPreferencesIn> {
    override val name = "SetRoomCameraPreferencesIn"

    override fun read(r: PacketReader): SetRoomCameraPreferencesIn = readSetRoomCameraPreferencesIn(r)

    override fun write(w: PacketWriter, m: SetRoomCameraPreferencesIn) = writeSetRoomCameraPreferencesIn(w, m)

    private fun readSetRoomCameraPreferencesIn(r: PacketReader): SetRoomCameraPreferencesIn {
        val vBlockFollow = r.boolean()
        return SetRoomCameraPreferencesIn(vBlockFollow)
    }

    private fun writeSetRoomCameraPreferencesIn(w: PacketWriter, m: SetRoomCameraPreferencesIn) {
        w.boolean(m.blockFollow)
    }
}
