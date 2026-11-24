package zel.protocol.generated

data class InterstitialOut(
    val boolean1: Boolean,
)

object InterstitialOutCodec : OutgoingCodec<InterstitialOut> {
    override val name = "InterstitialOut"

    override fun read(r: PacketReader): InterstitialOut = readInterstitialOut(r)

    override fun write(w: PacketWriter, m: InterstitialOut) = writeInterstitialOut(w, m)

    private fun readInterstitialOut(r: PacketReader): InterstitialOut {
        val vBoolean1 = r.boolean()
        return InterstitialOut(vBoolean1)
    }

    private fun writeInterstitialOut(w: PacketWriter, m: InterstitialOut) {
        w.boolean(m.boolean1)
    }
}
