package zel.protocol.generated

data class WiredSetRoomSettingsIn(
    val modifyMask: Int,
    val inspectMask: Int,
    val timezone: String,
)

object WiredSetRoomSettingsInCodec : IncomingCodec<WiredSetRoomSettingsIn> {
    override val name = "WiredSetRoomSettingsIn"

    override fun read(r: PacketReader): WiredSetRoomSettingsIn = readWiredSetRoomSettingsIn(r)

    override fun write(w: PacketWriter, m: WiredSetRoomSettingsIn) = writeWiredSetRoomSettingsIn(w, m)

    private fun readWiredSetRoomSettingsIn(r: PacketReader): WiredSetRoomSettingsIn {
        val vModifyMask = r.int()
        val vInspectMask = r.int()
        val vTimezone = r.string()
        return WiredSetRoomSettingsIn(vModifyMask, vInspectMask, vTimezone)
    }

    private fun writeWiredSetRoomSettingsIn(w: PacketWriter, m: WiredSetRoomSettingsIn) {
        w.int(m.modifyMask)
        w.int(m.inspectMask)
        w.string(m.timezone)
    }
}
