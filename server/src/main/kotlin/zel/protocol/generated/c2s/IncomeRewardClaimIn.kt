package zel.protocol.generated

data class IncomeRewardClaimIn(
    val byte1: Byte,
)

object IncomeRewardClaimInCodec : IncomingCodec<IncomeRewardClaimIn> {
    override val name = "IncomeRewardClaimIn"

    override fun read(r: PacketReader): IncomeRewardClaimIn = readIncomeRewardClaimIn(r)

    override fun write(w: PacketWriter, m: IncomeRewardClaimIn) = writeIncomeRewardClaimIn(w, m)

    private fun readIncomeRewardClaimIn(r: PacketReader): IncomeRewardClaimIn {
        val vByte1 = r.byte()
        return IncomeRewardClaimIn(vByte1)
    }

    private fun writeIncomeRewardClaimIn(w: PacketWriter, m: IncomeRewardClaimIn) {
        w.byte(m.byte1)
    }
}
