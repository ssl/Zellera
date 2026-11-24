package zel.protocol.generated

data class WithdrawAllFromChestIn(
    val itemId: Int,
)

object WithdrawAllFromChestInCodec : IncomingCodec<WithdrawAllFromChestIn> {
    override val name = "WithdrawAllFromChestIn"

    override fun read(r: PacketReader): WithdrawAllFromChestIn = readWithdrawAllFromChestIn(r)

    override fun write(w: PacketWriter, m: WithdrawAllFromChestIn) = writeWithdrawAllFromChestIn(w, m)

    private fun readWithdrawAllFromChestIn(r: PacketReader): WithdrawAllFromChestIn {
        val vItemId = r.int()
        return WithdrawAllFromChestIn(vItemId)
    }

    private fun writeWithdrawAllFromChestIn(w: PacketWriter, m: WithdrawAllFromChestIn) {
        w.int(m.itemId)
    }
}
