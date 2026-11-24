package zel.protocol.generated

data class WithdrawItemsFromChestIn(
    val int1: Int,
    val int2: Int,
)

object WithdrawItemsFromChestInCodec : IncomingCodec<WithdrawItemsFromChestIn> {
    override val name = "WithdrawItemsFromChestIn"

    override fun read(r: PacketReader): WithdrawItemsFromChestIn = readWithdrawItemsFromChestIn(r)

    override fun write(w: PacketWriter, m: WithdrawItemsFromChestIn) = writeWithdrawItemsFromChestIn(w, m)

    private fun readWithdrawItemsFromChestIn(r: PacketReader): WithdrawItemsFromChestIn {
        val vInt1 = r.int()
        val vInt2 = r.int()
        return WithdrawItemsFromChestIn(vInt1, vInt2)
    }

    private fun writeWithdrawItemsFromChestIn(w: PacketWriter, m: WithdrawItemsFromChestIn) {
        w.int(m.int1)
        w.int(m.int2)
    }
}
