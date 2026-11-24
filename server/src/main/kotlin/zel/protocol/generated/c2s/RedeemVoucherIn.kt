package zel.protocol.generated

data class RedeemVoucherIn(
    val voucherCode: String,
)

object RedeemVoucherInCodec : IncomingCodec<RedeemVoucherIn> {
    override val name = "RedeemVoucherIn"

    override fun read(r: PacketReader): RedeemVoucherIn = readRedeemVoucherIn(r)

    override fun write(w: PacketWriter, m: RedeemVoucherIn) = writeRedeemVoucherIn(w, m)

    private fun readRedeemVoucherIn(r: PacketReader): RedeemVoucherIn {
        val vVoucherCode = r.string()
        return RedeemVoucherIn(vVoucherCode)
    }

    private fun writeRedeemVoucherIn(w: PacketWriter, m: RedeemVoucherIn) {
        w.string(m.voucherCode)
    }
}
