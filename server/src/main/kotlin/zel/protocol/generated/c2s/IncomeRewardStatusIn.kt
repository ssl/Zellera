package zel.protocol.generated

data object IncomeRewardStatusIn

object IncomeRewardStatusInCodec : IncomingCodec<IncomeRewardStatusIn> {
    override val name = "IncomeRewardStatusIn"

    override fun read(r: PacketReader): IncomeRewardStatusIn = IncomeRewardStatusIn

    override fun write(w: PacketWriter, m: IncomeRewardStatusIn) = writeIncomeRewardStatusIn(w, m)

    @Suppress("UNUSED_PARAMETER")
    private fun writeIncomeRewardStatusIn(w: PacketWriter, m: IncomeRewardStatusIn) {
    }
}
