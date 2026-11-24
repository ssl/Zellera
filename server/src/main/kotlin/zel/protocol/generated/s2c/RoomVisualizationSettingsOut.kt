package zel.protocol.generated

data class RoomVisualizationSettingsOut(
    val boolean1: Boolean,
    val int1: Int,
    val int2: Int,
)

object RoomVisualizationSettingsOutCodec : OutgoingCodec<RoomVisualizationSettingsOut> {
    override val name = "RoomVisualizationSettingsOut"

    override fun read(r: PacketReader): RoomVisualizationSettingsOut = readRoomVisualizationSettingsOut(r)

    override fun write(w: PacketWriter, m: RoomVisualizationSettingsOut) = writeRoomVisualizationSettingsOut(w, m)

    private fun readRoomVisualizationSettingsOut(r: PacketReader): RoomVisualizationSettingsOut {
        val vBoolean1 = r.boolean()
        val vInt1 = r.int()
        val vInt2 = r.int()
        return RoomVisualizationSettingsOut(vBoolean1, vInt1, vInt2)
    }

    private fun writeRoomVisualizationSettingsOut(w: PacketWriter, m: RoomVisualizationSettingsOut) {
        w.boolean(m.boolean1)
        w.int(m.int1)
        w.int(m.int2)
    }
}
