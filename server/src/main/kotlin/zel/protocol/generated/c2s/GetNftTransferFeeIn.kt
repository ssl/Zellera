package zel.protocol.generated

data object GetNftTransferFeeIn

object GetNftTransferFeeInCodec : IncomingCodec<GetNftTransferFeeIn> {
    override val name = "GetNftTransferFeeIn"

    override fun read(r: PacketReader): GetNftTransferFeeIn = GetNftTransferFeeIn

    override fun write(w: PacketWriter, m: GetNftTransferFeeIn) = writeGetNftTransferFeeIn(w, m)

    @Suppress("UNUSED_PARAMETER")
    private fun writeGetNftTransferFeeIn(w: PacketWriter, m: GetNftTransferFeeIn) {
    }
}
