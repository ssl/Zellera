package zel.protocol.generated

data class CreditVaultStatusOut(
    val boolean1: Boolean,
    val int1: Int,
    val int2: Int,
)

object CreditVaultStatusOutCodec : OutgoingCodec<CreditVaultStatusOut> {
    override val name = "CreditVaultStatusOut"

    override fun read(r: PacketReader): CreditVaultStatusOut = readCreditVaultStatusOut(r)

    override fun write(w: PacketWriter, m: CreditVaultStatusOut) = writeCreditVaultStatusOut(w, m)

    private fun readCreditVaultStatusOut(r: PacketReader): CreditVaultStatusOut {
        val vBoolean1 = r.boolean()
        val vInt1 = r.int()
        val vInt2 = r.int()
        return CreditVaultStatusOut(vBoolean1, vInt1, vInt2)
    }

    private fun writeCreditVaultStatusOut(w: PacketWriter, m: CreditVaultStatusOut) {
        w.boolean(m.boolean1)
        w.int(m.int1)
        w.int(m.int2)
    }
}
