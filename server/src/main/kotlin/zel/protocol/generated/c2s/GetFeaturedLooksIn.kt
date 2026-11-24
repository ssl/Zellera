package zel.protocol.generated

data class GetFeaturedLooksIn(
    val count: Byte,
)

object GetFeaturedLooksInCodec : IncomingCodec<GetFeaturedLooksIn> {
    override val name = "GetFeaturedLooksIn"

    override fun read(r: PacketReader): GetFeaturedLooksIn = readGetFeaturedLooksIn(r)

    override fun write(w: PacketWriter, m: GetFeaturedLooksIn) = writeGetFeaturedLooksIn(w, m)

    private fun readGetFeaturedLooksIn(r: PacketReader): GetFeaturedLooksIn {
        val vCount = r.byte()
        return GetFeaturedLooksIn(vCount)
    }

    private fun writeGetFeaturedLooksIn(w: PacketWriter, m: GetFeaturedLooksIn) {
        w.byte(m.count)
    }
}
