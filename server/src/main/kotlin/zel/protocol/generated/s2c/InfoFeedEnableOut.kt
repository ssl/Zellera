package zel.protocol.generated

data class InfoFeedEnableOut(
    val boolean1: Boolean,
)

object InfoFeedEnableOutCodec : OutgoingCodec<InfoFeedEnableOut> {
    override val name = "InfoFeedEnableOut"

    override fun read(r: PacketReader): InfoFeedEnableOut = readInfoFeedEnableOut(r)

    override fun write(w: PacketWriter, m: InfoFeedEnableOut) = writeInfoFeedEnableOut(w, m)

    private fun readInfoFeedEnableOut(r: PacketReader): InfoFeedEnableOut {
        val vBoolean1 = r.boolean()
        return InfoFeedEnableOut(vBoolean1)
    }

    private fun writeInfoFeedEnableOut(w: PacketWriter, m: InfoFeedEnableOut) {
        w.boolean(m.boolean1)
    }
}
