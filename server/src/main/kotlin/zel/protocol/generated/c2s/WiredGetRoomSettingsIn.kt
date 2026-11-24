package zel.protocol.generated

data object WiredGetRoomSettingsIn

object WiredGetRoomSettingsInCodec : IncomingCodec<WiredGetRoomSettingsIn> {
    override val name = "WiredGetRoomSettingsIn"

    override fun read(r: PacketReader): WiredGetRoomSettingsIn = WiredGetRoomSettingsIn

    override fun write(w: PacketWriter, m: WiredGetRoomSettingsIn) = writeWiredGetRoomSettingsIn(w, m)

    @Suppress("UNUSED_PARAMETER")
    private fun writeWiredGetRoomSettingsIn(w: PacketWriter, m: WiredGetRoomSettingsIn) {
    }
}
