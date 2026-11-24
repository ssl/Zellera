package zel.protocol.generated

data class WiredPermissionsOut(
    val boolean1: Boolean,
    val boolean2: Boolean,
)

object WiredPermissionsOutCodec : OutgoingCodec<WiredPermissionsOut> {
    override val name = "WiredPermissionsOut"

    override fun read(r: PacketReader): WiredPermissionsOut = readWiredPermissionsOut(r)

    override fun write(w: PacketWriter, m: WiredPermissionsOut) = writeWiredPermissionsOut(w, m)

    private fun readWiredPermissionsOut(r: PacketReader): WiredPermissionsOut {
        val vBoolean1 = r.boolean()
        val vBoolean2 = r.boolean()
        return WiredPermissionsOut(vBoolean1, vBoolean2)
    }

    private fun writeWiredPermissionsOut(w: PacketWriter, m: WiredPermissionsOut) {
        w.boolean(m.boolean1)
        w.boolean(m.boolean2)
    }
}
