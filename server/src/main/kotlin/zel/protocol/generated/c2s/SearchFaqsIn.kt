package zel.protocol.generated

data class SearchFaqsIn(
    val string1: String,
)

object SearchFaqsInCodec : IncomingCodec<SearchFaqsIn> {
    override val name = "SearchFaqsIn"

    override fun read(r: PacketReader): SearchFaqsIn = readSearchFaqsIn(r)

    override fun write(w: PacketWriter, m: SearchFaqsIn) = writeSearchFaqsIn(w, m)

    private fun readSearchFaqsIn(r: PacketReader): SearchFaqsIn {
        val vString1 = r.string()
        return SearchFaqsIn(vString1)
    }

    private fun writeSearchFaqsIn(w: PacketWriter, m: SearchFaqsIn) {
        w.string(m.string1)
    }
}
