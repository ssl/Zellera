package zel.protocol.generated

data class RoomChatSettingsOut(
    val int1: Int,
)

object RoomChatSettingsOutCodec : OutgoingCodec<RoomChatSettingsOut> {
    override val name = "RoomChatSettingsOut"

    override fun read(r: PacketReader): RoomChatSettingsOut = readRoomChatSettingsOut(r)

    override fun write(w: PacketWriter, m: RoomChatSettingsOut) = writeRoomChatSettingsOut(w, m)

    private fun readRoomChatSettingsOut(r: PacketReader): RoomChatSettingsOut {
        val vInt1 = r.int()
        return RoomChatSettingsOut(vInt1)
    }

    private fun writeRoomChatSettingsOut(w: PacketWriter, m: RoomChatSettingsOut) {
        w.int(m.int1)
    }
}
