package zel.protocol.generated

data class WithdrawCoinsFromChestIn(
    val int1: Int,
    val int2: Int,
)

object WithdrawCoinsFromChestInCodec : IncomingCodec<WithdrawCoinsFromChestIn> {
    override val name = "WithdrawCoinsFromChestIn"

    override fun read(r: PacketReader): WithdrawCoinsFromChestIn = readWithdrawCoinsFromChestIn(r)

    override fun write(w: PacketWriter, m: WithdrawCoinsFromChestIn) = writeWithdrawCoinsFromChestIn(w, m)

    private fun readWithdrawCoinsFromChestIn(r: PacketReader): WithdrawCoinsFromChestIn {
        val vInt1 = r.int()
        val vInt2 = r.int()
        return WithdrawCoinsFromChestIn(vInt1, vInt2)
    }

    private fun writeWithdrawCoinsFromChestIn(w: PacketWriter, m: WithdrawCoinsFromChestIn) {
        w.int(m.int1)
        w.int(m.int2)
    }
}
