package zel.protocol.generated

data object WithdrawCreditVaultIn

object WithdrawCreditVaultInCodec : IncomingCodec<WithdrawCreditVaultIn> {
    override val name = "WithdrawCreditVaultIn"

    override fun read(r: PacketReader): WithdrawCreditVaultIn = WithdrawCreditVaultIn

    override fun write(w: PacketWriter, m: WithdrawCreditVaultIn) = writeWithdrawCreditVaultIn(w, m)

    @Suppress("UNUSED_PARAMETER")
    private fun writeWithdrawCreditVaultIn(w: PacketWriter, m: WithdrawCreditVaultIn) {
    }
}
