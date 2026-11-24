package zel.protocol.generated

data class WiredClickSettingsOut(
    val int1: Int,
    val int2: Int,
)

object WiredClickSettingsOutCodec : OutgoingCodec<WiredClickSettingsOut> {
    override val name = "WiredClickSettingsOut"

    override fun read(r: PacketReader): WiredClickSettingsOut = readWiredClickSettingsOut(r)

    override fun write(w: PacketWriter, m: WiredClickSettingsOut) = writeWiredClickSettingsOut(w, m)

    private fun readWiredClickSettingsOut(r: PacketReader): WiredClickSettingsOut {
        val vInt1 = r.int()
        val vInt2 = r.int()
        return WiredClickSettingsOut(vInt1, vInt2)
    }

    private fun writeWiredClickSettingsOut(w: PacketWriter, m: WiredClickSettingsOut) {
        w.int(m.int1)
        w.int(m.int2)
    }
}
