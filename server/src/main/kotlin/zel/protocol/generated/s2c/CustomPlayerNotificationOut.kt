package zel.protocol.generated

data class CustomPlayerNotificationOut(
    val int1: Int,
)

object CustomPlayerNotificationOutCodec : OutgoingCodec<CustomPlayerNotificationOut> {
    override val name = "CustomPlayerNotificationOut"

    override fun read(r: PacketReader): CustomPlayerNotificationOut = readCustomPlayerNotificationOut(r)

    override fun write(w: PacketWriter, m: CustomPlayerNotificationOut) = writeCustomPlayerNotificationOut(w, m)

    private fun readCustomPlayerNotificationOut(r: PacketReader): CustomPlayerNotificationOut {
        val vInt1 = r.int()
        return CustomPlayerNotificationOut(vInt1)
    }

    private fun writeCustomPlayerNotificationOut(w: PacketWriter, m: CustomPlayerNotificationOut) {
        w.int(m.int1)
    }
}
