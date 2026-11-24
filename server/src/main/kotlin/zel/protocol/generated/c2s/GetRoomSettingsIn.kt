package zel.protocol.generated

data class GetRoomSettingsIn(
    val roomId: Int,
)

object GetRoomSettingsInCodec : IncomingCodec<GetRoomSettingsIn> {
    override val name = "GetRoomSettingsIn"

    override fun read(r: PacketReader): GetRoomSettingsIn = readGetRoomSettingsIn(r)

    override fun write(w: PacketWriter, m: GetRoomSettingsIn) = writeGetRoomSettingsIn(w, m)

    private fun readGetRoomSettingsIn(r: PacketReader): GetRoomSettingsIn {
        val vRoomId = r.int()
        return GetRoomSettingsIn(vRoomId)
    }

    private fun writeGetRoomSettingsIn(w: PacketWriter, m: GetRoomSettingsIn) {
        w.int(m.roomId)
    }
}
