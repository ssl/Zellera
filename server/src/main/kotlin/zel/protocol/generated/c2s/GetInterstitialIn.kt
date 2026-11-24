package zel.protocol.generated

data object GetInterstitialIn

object GetInterstitialInCodec : IncomingCodec<GetInterstitialIn> {
    override val name = "GetInterstitialIn"

    override fun read(r: PacketReader): GetInterstitialIn = GetInterstitialIn

    override fun write(w: PacketWriter, m: GetInterstitialIn) = writeGetInterstitialIn(w, m)

    @Suppress("UNUSED_PARAMETER")
    private fun writeGetInterstitialIn(w: PacketWriter, m: GetInterstitialIn) {
    }
}
