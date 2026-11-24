package zel.protocol.generated

data object GuideSessionRequesterCancelsIn

object GuideSessionRequesterCancelsInCodec : IncomingCodec<GuideSessionRequesterCancelsIn> {
    override val name = "GuideSessionRequesterCancelsIn"

    override fun read(r: PacketReader): GuideSessionRequesterCancelsIn = GuideSessionRequesterCancelsIn

    override fun write(w: PacketWriter, m: GuideSessionRequesterCancelsIn) = writeGuideSessionRequesterCancelsIn(w, m)

    @Suppress("UNUSED_PARAMETER")
    private fun writeGuideSessionRequesterCancelsIn(w: PacketWriter, m: GuideSessionRequesterCancelsIn) {
    }
}
