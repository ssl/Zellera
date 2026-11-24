package zel.protocol.generated

data class NftTransferFeeOut(
    val int1: Int,
)

object NftTransferFeeOutCodec : OutgoingCodec<NftTransferFeeOut> {
    override val name = "NftTransferFeeOut"

    override fun read(r: PacketReader): NftTransferFeeOut = readNftTransferFeeOut(r)

    override fun write(w: PacketWriter, m: NftTransferFeeOut) = writeNftTransferFeeOut(w, m)

    private fun readNftTransferFeeOut(r: PacketReader): NftTransferFeeOut {
        val vInt1 = r.int()
        return NftTransferFeeOut(vInt1)
    }

    private fun writeNftTransferFeeOut(w: PacketWriter, m: NftTransferFeeOut) {
        w.int(m.int1)
    }
}
