package zel.protocol.generated

data class RoomMessageNotificationOut(
    val int1: Int,
    val string1: String,
    val int2: Int,
)

object RoomMessageNotificationOutCodec : OutgoingCodec<RoomMessageNotificationOut> {
    override val name = "RoomMessageNotificationOut"

    override fun read(r: PacketReader): RoomMessageNotificationOut = readRoomMessageNotificationOut(r)

    override fun write(w: PacketWriter, m: RoomMessageNotificationOut) = writeRoomMessageNotificationOut(w, m)

    private fun readRoomMessageNotificationOut(r: PacketReader): RoomMessageNotificationOut {
        val vInt1 = r.int()
        val vString1 = r.string()
        val vInt2 = r.int()
        return RoomMessageNotificationOut(vInt1, vString1, vInt2)
    }

    private fun writeRoomMessageNotificationOut(w: PacketWriter, m: RoomMessageNotificationOut) {
        w.int(m.int1)
        w.string(m.string1)
        w.int(m.int2)
    }
}
