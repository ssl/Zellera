package zel.protocol.generated

data class WiredRoomSettingsOut(
    val int1: Int,
    val int2: Int,
    val string1: String,
)

object WiredRoomSettingsOutCodec : OutgoingCodec<WiredRoomSettingsOut> {
    override val name = "WiredRoomSettingsOut"

    override fun read(r: PacketReader): WiredRoomSettingsOut = readWiredRoomSettingsOut(r)

    override fun write(w: PacketWriter, m: WiredRoomSettingsOut) = writeWiredRoomSettingsOut(w, m)

    private fun readWiredRoomSettingsOut(r: PacketReader): WiredRoomSettingsOut {
        val vInt1 = r.int()
        val vInt2 = r.int()
        val vString1 = r.string()
        return WiredRoomSettingsOut(vInt1, vInt2, vString1)
    }

    private fun writeWiredRoomSettingsOut(w: PacketWriter, m: WiredRoomSettingsOut) {
        w.int(m.int1)
        w.int(m.int2)
        w.string(m.string1)
    }
}
