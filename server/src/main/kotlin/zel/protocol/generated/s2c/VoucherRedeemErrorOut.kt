package zel.protocol.generated

data class VoucherRedeemErrorOut(
    val string1: String,
)

object VoucherRedeemErrorOutCodec : OutgoingCodec<VoucherRedeemErrorOut> {
    override val name = "VoucherRedeemErrorOut"

    override fun read(r: PacketReader): VoucherRedeemErrorOut = readVoucherRedeemErrorOut(r)

    override fun write(w: PacketWriter, m: VoucherRedeemErrorOut) = writeVoucherRedeemErrorOut(w, m)

    private fun readVoucherRedeemErrorOut(r: PacketReader): VoucherRedeemErrorOut {
        val vString1 = r.string()
        return VoucherRedeemErrorOut(vString1)
    }

    private fun writeVoucherRedeemErrorOut(w: PacketWriter, m: VoucherRedeemErrorOut) {
        w.string(m.string1)
    }
}
