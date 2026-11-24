package zel.protocol.generated

data class MaintenanceStatusOut(
    val boolean1: Boolean,
    val int1: Int,
    val trailing1: Int?,
)

object MaintenanceStatusOutCodec : OutgoingCodec<MaintenanceStatusOut> {
    override val name = "MaintenanceStatusOut"

    override fun read(r: PacketReader): MaintenanceStatusOut = readMaintenanceStatusOut(r)

    override fun write(w: PacketWriter, m: MaintenanceStatusOut) = writeMaintenanceStatusOut(w, m)

    private fun readMaintenanceStatusOut(r: PacketReader): MaintenanceStatusOut {
        val vBoolean1 = r.boolean()
        val vInt1 = r.int()
        val vTrailing1 = if (r.remaining > 0) r.int() else null
        return MaintenanceStatusOut(vBoolean1, vInt1, vTrailing1)
    }

    private fun writeMaintenanceStatusOut(w: PacketWriter, m: MaintenanceStatusOut) {
        w.boolean(m.boolean1)
        w.int(m.int1)
        m.trailing1?.let { w.int(it) }
    }
}
