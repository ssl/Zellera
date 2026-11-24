package zel.protocol.generated

data object InterstitialShownIn

object InterstitialShownInCodec : IncomingCodec<InterstitialShownIn> {
    override val name = "InterstitialShownIn"

    override fun read(r: PacketReader): InterstitialShownIn = InterstitialShownIn

    override fun write(w: PacketWriter, m: InterstitialShownIn) = writeInterstitialShownIn(w, m)

    @Suppress("UNUSED_PARAMETER")
    private fun writeInterstitialShownIn(w: PacketWriter, m: InterstitialShownIn) {
    }
}
