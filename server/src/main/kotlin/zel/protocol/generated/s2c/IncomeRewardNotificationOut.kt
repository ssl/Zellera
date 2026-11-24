package zel.protocol.generated

data class IncomeRewardNotificationOut(
    val byte1: Byte,
)

object IncomeRewardNotificationOutCodec : OutgoingCodec<IncomeRewardNotificationOut> {
    override val name = "IncomeRewardNotificationOut"

    override fun read(r: PacketReader): IncomeRewardNotificationOut = readIncomeRewardNotificationOut(r)

    override fun write(w: PacketWriter, m: IncomeRewardNotificationOut) = writeIncomeRewardNotificationOut(w, m)

    private fun readIncomeRewardNotificationOut(r: PacketReader): IncomeRewardNotificationOut {
        val vByte1 = r.byte()
        return IncomeRewardNotificationOut(vByte1)
    }

    private fun writeIncomeRewardNotificationOut(w: PacketWriter, m: IncomeRewardNotificationOut) {
        w.byte(m.byte1)
    }
}
