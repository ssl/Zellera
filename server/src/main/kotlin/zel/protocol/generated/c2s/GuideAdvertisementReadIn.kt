package zel.protocol.generated

data object GuideAdvertisementReadIn

object GuideAdvertisementReadInCodec : IncomingCodec<GuideAdvertisementReadIn> {
    override val name = "GuideAdvertisementReadIn"

    override fun read(r: PacketReader): GuideAdvertisementReadIn = GuideAdvertisementReadIn

    override fun write(w: PacketWriter, m: GuideAdvertisementReadIn) = writeGuideAdvertisementReadIn(w, m)

    @Suppress("UNUSED_PARAMETER")
    private fun writeGuideAdvertisementReadIn(w: PacketWriter, m: GuideAdvertisementReadIn) {
    }
}
