package zel.protocol.generated

data class ModToolPreferencesIn(
    val x: Int,
    val y: Int,
    val width: Int,
    val height: Int,
)

object ModToolPreferencesInCodec : IncomingCodec<ModToolPreferencesIn> {
    override val name = "ModToolPreferencesIn"

    override fun read(r: PacketReader): ModToolPreferencesIn = readModToolPreferencesIn(r)

    override fun write(w: PacketWriter, m: ModToolPreferencesIn) = writeModToolPreferencesIn(w, m)

    private fun readModToolPreferencesIn(r: PacketReader): ModToolPreferencesIn {
        val vX = r.int()
        val vY = r.int()
        val vWidth = r.int()
        val vHeight = r.int()
        return ModToolPreferencesIn(vX, vY, vWidth, vHeight)
    }

    private fun writeModToolPreferencesIn(w: PacketWriter, m: ModToolPreferencesIn) {
        w.int(m.x)
        w.int(m.y)
        w.int(m.width)
        w.int(m.height)
    }
}
