package zel.protocol.generated

data class RespectNotificationOut(
    val int1: Int,
    val int2: Int,
)

object RespectNotificationOutCodec : OutgoingCodec<RespectNotificationOut> {
    override val name = "RespectNotificationOut"

    override fun read(r: PacketReader): RespectNotificationOut = readRespectNotificationOut(r)

    override fun write(w: PacketWriter, m: RespectNotificationOut) = writeRespectNotificationOut(w, m)

    private fun readRespectNotificationOut(r: PacketReader): RespectNotificationOut {
        val vInt1 = r.int()
        val vInt2 = r.int()
        return RespectNotificationOut(vInt1, vInt2)
    }

    private fun writeRespectNotificationOut(w: PacketWriter, m: RespectNotificationOut) {
        w.int(m.int1)
        w.int(m.int2)
    }
}
