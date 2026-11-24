package zel.protocol.generated

data class NavigatorSettingsOut(
    val int1: Int,
    val int2: Int,
)

object NavigatorSettingsOutCodec : OutgoingCodec<NavigatorSettingsOut> {
    override val name = "NavigatorSettingsOut"

    override fun read(r: PacketReader): NavigatorSettingsOut = readNavigatorSettingsOut(r)

    override fun write(w: PacketWriter, m: NavigatorSettingsOut) = writeNavigatorSettingsOut(w, m)

    private fun readNavigatorSettingsOut(r: PacketReader): NavigatorSettingsOut {
        val vInt1 = r.int()
        val vInt2 = r.int()
        return NavigatorSettingsOut(vInt1, vInt2)
    }

    private fun writeNavigatorSettingsOut(w: PacketWriter, m: NavigatorSettingsOut) {
        w.int(m.int1)
        w.int(m.int2)
    }
}
