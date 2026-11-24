package zel.protocol.generated

data class MOTDNotificationOut(
    val list1: List<String>,
)

object MOTDNotificationOutCodec : OutgoingCodec<MOTDNotificationOut> {
    override val name = "MOTDNotificationOut"

    override fun read(r: PacketReader): MOTDNotificationOut = readMOTDNotificationOut(r)

    override fun write(w: PacketWriter, m: MOTDNotificationOut) = writeMOTDNotificationOut(w, m)

    private fun readMOTDNotificationOut(r: PacketReader): MOTDNotificationOut {
        val vList1 = List(r.countInt(2)) { r.string() }
        return MOTDNotificationOut(vList1)
    }

    private fun writeMOTDNotificationOut(w: PacketWriter, m: MOTDNotificationOut) {
        w.countInt(m.list1.size)
        m.list1.forEach { w.string(it) }
    }
}
