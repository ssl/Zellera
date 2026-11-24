package zel.protocol.generated

data class IncomeRewardClaimResponseOut(
    val byte1: Byte,
    val boolean1: Boolean,
)

object IncomeRewardClaimResponseOutCodec : OutgoingCodec<IncomeRewardClaimResponseOut> {
    override val name = "IncomeRewardClaimResponseOut"

    override fun read(r: PacketReader): IncomeRewardClaimResponseOut = readIncomeRewardClaimResponseOut(r)

    override fun write(w: PacketWriter, m: IncomeRewardClaimResponseOut) = writeIncomeRewardClaimResponseOut(w, m)

    private fun readIncomeRewardClaimResponseOut(r: PacketReader): IncomeRewardClaimResponseOut {
        val vByte1 = r.byte()
        val vBoolean1 = r.boolean()
        return IncomeRewardClaimResponseOut(vByte1, vBoolean1)
    }

    private fun writeIncomeRewardClaimResponseOut(w: PacketWriter, m: IncomeRewardClaimResponseOut) {
        w.byte(m.byte1)
        w.boolean(m.boolean1)
    }
}
