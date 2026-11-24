package zel.protocol.generated

data class NewNavigatorPreferencesOut(
    val int1: Int,
    val int2: Int,
    val int3: Int,
    val int4: Int,
    val boolean1: Boolean,
    val int5: Int,
)

object NewNavigatorPreferencesOutCodec : OutgoingCodec<NewNavigatorPreferencesOut> {
    override val name = "NewNavigatorPreferencesOut"

    override fun read(r: PacketReader): NewNavigatorPreferencesOut = readNewNavigatorPreferencesOut(r)

    override fun write(w: PacketWriter, m: NewNavigatorPreferencesOut) = writeNewNavigatorPreferencesOut(w, m)

    private fun readNewNavigatorPreferencesOut(r: PacketReader): NewNavigatorPreferencesOut {
        val vInt1 = r.int()
        val vInt2 = r.int()
        val vInt3 = r.int()
        val vInt4 = r.int()
        val vBoolean1 = r.boolean()
        val vInt5 = r.int()
        return NewNavigatorPreferencesOut(vInt1, vInt2, vInt3, vInt4, vBoolean1, vInt5)
    }

    private fun writeNewNavigatorPreferencesOut(w: PacketWriter, m: NewNavigatorPreferencesOut) {
        w.int(m.int1)
        w.int(m.int2)
        w.int(m.int3)
        w.int(m.int4)
        w.boolean(m.boolean1)
        w.int(m.int5)
    }
}
