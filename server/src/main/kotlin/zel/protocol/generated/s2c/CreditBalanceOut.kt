package zel.protocol.generated

data class CreditBalanceOut(
    val string1: String,
)

object CreditBalanceOutCodec : OutgoingCodec<CreditBalanceOut> {
    override val name = "CreditBalanceOut"

    override fun read(r: PacketReader): CreditBalanceOut = readCreditBalanceOut(r)

    override fun write(w: PacketWriter, m: CreditBalanceOut) = writeCreditBalanceOut(w, m)

    private fun readCreditBalanceOut(r: PacketReader): CreditBalanceOut {
        val vString1 = r.string()
        return CreditBalanceOut(vString1)
    }

    private fun writeCreditBalanceOut(w: PacketWriter, m: CreditBalanceOut) {
        w.string(m.string1)
    }
}
