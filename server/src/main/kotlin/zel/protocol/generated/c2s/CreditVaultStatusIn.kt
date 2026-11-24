package zel.protocol.generated

data object CreditVaultStatusIn

object CreditVaultStatusInCodec : IncomingCodec<CreditVaultStatusIn> {
    override val name = "CreditVaultStatusIn"

    override fun read(r: PacketReader): CreditVaultStatusIn = CreditVaultStatusIn

    override fun write(w: PacketWriter, m: CreditVaultStatusIn) = writeCreditVaultStatusIn(w, m)

    @Suppress("UNUSED_PARAMETER")
    private fun writeCreditVaultStatusIn(w: PacketWriter, m: CreditVaultStatusIn) {
    }
}
