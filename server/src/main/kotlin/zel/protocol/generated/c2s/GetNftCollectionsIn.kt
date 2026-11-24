package zel.protocol.generated

data class GetNftCollectionsIn(
    val string1: String,
)

object GetNftCollectionsInCodec : IncomingCodec<GetNftCollectionsIn> {
    override val name = "GetNftCollectionsIn"

    override fun read(r: PacketReader): GetNftCollectionsIn = readGetNftCollectionsIn(r)

    override fun write(w: PacketWriter, m: GetNftCollectionsIn) = writeGetNftCollectionsIn(w, m)

    private fun readGetNftCollectionsIn(r: PacketReader): GetNftCollectionsIn {
        val vString1 = r.string()
        return GetNftCollectionsIn(vString1)
    }

    private fun writeGetNftCollectionsIn(w: PacketWriter, m: GetNftCollectionsIn) {
        w.string(m.string1)
    }
}
