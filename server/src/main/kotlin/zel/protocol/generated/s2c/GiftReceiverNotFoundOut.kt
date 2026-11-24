package zel.protocol.generated

data object GiftReceiverNotFoundOut

object GiftReceiverNotFoundOutCodec : OutgoingCodec<GiftReceiverNotFoundOut> {
    override val name = "GiftReceiverNotFoundOut"

    override fun read(r: PacketReader): GiftReceiverNotFoundOut = GiftReceiverNotFoundOut

    override fun write(w: PacketWriter, m: GiftReceiverNotFoundOut) = writeGiftReceiverNotFoundOut(w, m)

    @Suppress("UNUSED_PARAMETER")
    private fun writeGiftReceiverNotFoundOut(w: PacketWriter, m: GiftReceiverNotFoundOut) {
    }
}
