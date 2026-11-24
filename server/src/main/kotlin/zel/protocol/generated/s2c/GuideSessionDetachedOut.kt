package zel.protocol.generated

data object GuideSessionDetachedOut

object GuideSessionDetachedOutCodec : OutgoingCodec<GuideSessionDetachedOut> {
    override val name = "GuideSessionDetachedOut"

    override fun read(r: PacketReader): GuideSessionDetachedOut = GuideSessionDetachedOut

    override fun write(w: PacketWriter, m: GuideSessionDetachedOut) = writeGuideSessionDetachedOut(w, m)

    @Suppress("UNUSED_PARAMETER")
    private fun writeGuideSessionDetachedOut(w: PacketWriter, m: GuideSessionDetachedOut) {
    }
}
