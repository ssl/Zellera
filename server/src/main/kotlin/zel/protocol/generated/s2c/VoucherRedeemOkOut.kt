package zel.protocol.generated

data class VoucherRedeemOkOut(
    val string1: String,
    val string2: String,
)

object VoucherRedeemOkOutCodec : OutgoingCodec<VoucherRedeemOkOut> {
    override val name = "VoucherRedeemOkOut"

    override fun read(r: PacketReader): VoucherRedeemOkOut = readVoucherRedeemOkOut(r)

    override fun write(w: PacketWriter, m: VoucherRedeemOkOut) = writeVoucherRedeemOkOut(w, m)

    private fun readVoucherRedeemOkOut(r: PacketReader): VoucherRedeemOkOut {
        val vString1 = r.string()
        val vString2 = r.string()
        return VoucherRedeemOkOut(vString1, vString2)
    }

    private fun writeVoucherRedeemOkOut(w: PacketWriter, m: VoucherRedeemOkOut) {
        w.string(m.string1)
        w.string(m.string2)
    }
}
