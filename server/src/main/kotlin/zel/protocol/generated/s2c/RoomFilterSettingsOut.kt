package zel.protocol.generated

data class RoomFilterSettingsOut(
    val list1: List<String>,
)

object RoomFilterSettingsOutCodec : OutgoingCodec<RoomFilterSettingsOut> {
    override val name = "RoomFilterSettingsOut"

    override fun read(r: PacketReader): RoomFilterSettingsOut = readRoomFilterSettingsOut(r)

    override fun write(w: PacketWriter, m: RoomFilterSettingsOut) = writeRoomFilterSettingsOut(w, m)

    private fun readRoomFilterSettingsOut(r: PacketReader): RoomFilterSettingsOut {
        val vList1 = List(r.countInt(2)) { r.string() }
        return RoomFilterSettingsOut(vList1)
    }

    private fun writeRoomFilterSettingsOut(w: PacketWriter, m: RoomFilterSettingsOut) {
        w.countInt(m.list1.size)
        m.list1.forEach { w.string(it) }
    }
}
