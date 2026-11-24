package zel.protocol.generated

data object GuideSessionResolvedIn

object GuideSessionResolvedInCodec : IncomingCodec<GuideSessionResolvedIn> {
    override val name = "GuideSessionResolvedIn"

    override fun read(r: PacketReader): GuideSessionResolvedIn = GuideSessionResolvedIn

    override fun write(w: PacketWriter, m: GuideSessionResolvedIn) = writeGuideSessionResolvedIn(w, m)

    @Suppress("UNUSED_PARAMETER")
    private fun writeGuideSessionResolvedIn(w: PacketWriter, m: GuideSessionResolvedIn) {
    }
}
