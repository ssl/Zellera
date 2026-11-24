package zel.protocol.generated

data object ShowMysteryBoxWaitOut

object ShowMysteryBoxWaitOutCodec : OutgoingCodec<ShowMysteryBoxWaitOut> {
    override val name = "ShowMysteryBoxWaitOut"

    override fun read(r: PacketReader): ShowMysteryBoxWaitOut = ShowMysteryBoxWaitOut

    override fun write(w: PacketWriter, m: ShowMysteryBoxWaitOut) = writeShowMysteryBoxWaitOut(w, m)

    @Suppress("UNUSED_PARAMETER")
    private fun writeShowMysteryBoxWaitOut(w: PacketWriter, m: ShowMysteryBoxWaitOut) {
    }
}
