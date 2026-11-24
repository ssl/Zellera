package zel.protocol.generated

data object CancelMysteryBoxWaitOut

object CancelMysteryBoxWaitOutCodec : OutgoingCodec<CancelMysteryBoxWaitOut> {
    override val name = "CancelMysteryBoxWaitOut"

    override fun read(r: PacketReader): CancelMysteryBoxWaitOut = CancelMysteryBoxWaitOut

    override fun write(w: PacketWriter, m: CancelMysteryBoxWaitOut) = writeCancelMysteryBoxWaitOut(w, m)

    @Suppress("UNUSED_PARAMETER")
    private fun writeCancelMysteryBoxWaitOut(w: PacketWriter, m: CancelMysteryBoxWaitOut) {
    }
}
