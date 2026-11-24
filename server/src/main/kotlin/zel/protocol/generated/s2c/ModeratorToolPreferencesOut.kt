package zel.protocol.generated

data class ModeratorToolPreferencesOut(
    val int1: Int,
    val int2: Int,
    val int3: Int,
    val int4: Int,
)

object ModeratorToolPreferencesOutCodec : OutgoingCodec<ModeratorToolPreferencesOut> {
    override val name = "ModeratorToolPreferencesOut"

    override fun read(r: PacketReader): ModeratorToolPreferencesOut = readModeratorToolPreferencesOut(r)

    override fun write(w: PacketWriter, m: ModeratorToolPreferencesOut) = writeModeratorToolPreferencesOut(w, m)

    private fun readModeratorToolPreferencesOut(r: PacketReader): ModeratorToolPreferencesOut {
        val vInt1 = r.int()
        val vInt2 = r.int()
        val vInt3 = r.int()
        val vInt4 = r.int()
        return ModeratorToolPreferencesOut(vInt1, vInt2, vInt3, vInt4)
    }

    private fun writeModeratorToolPreferencesOut(w: PacketWriter, m: ModeratorToolPreferencesOut) {
        w.int(m.int1)
        w.int(m.int2)
        w.int(m.int3)
        w.int(m.int4)
    }
}
