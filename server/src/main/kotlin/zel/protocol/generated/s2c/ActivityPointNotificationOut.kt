package zel.protocol.generated

data class ActivityPointNotificationOut(
    val int1: Int,
    val int2: Int,
    val int3: Int,
)

object ActivityPointNotificationOutCodec : OutgoingCodec<ActivityPointNotificationOut> {
    override val name = "ActivityPointNotificationOut"

    override fun read(r: PacketReader): ActivityPointNotificationOut = readActivityPointNotificationOut(r)

    override fun write(w: PacketWriter, m: ActivityPointNotificationOut) = writeActivityPointNotificationOut(w, m)

    private fun readActivityPointNotificationOut(r: PacketReader): ActivityPointNotificationOut {
        val vInt1 = r.int()
        val vInt2 = r.int()
        val vInt3 = r.int()
        return ActivityPointNotificationOut(vInt1, vInt2, vInt3)
    }

    private fun writeActivityPointNotificationOut(w: PacketWriter, m: ActivityPointNotificationOut) {
        w.int(m.int1)
        w.int(m.int2)
        w.int(m.int3)
    }
}
