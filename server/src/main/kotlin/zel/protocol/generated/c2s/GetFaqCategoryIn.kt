package zel.protocol.generated

data class GetFaqCategoryIn(
    val int1: Int,
)

object GetFaqCategoryInCodec : IncomingCodec<GetFaqCategoryIn> {
    override val name = "GetFaqCategoryIn"

    override fun read(r: PacketReader): GetFaqCategoryIn = readGetFaqCategoryIn(r)

    override fun write(w: PacketWriter, m: GetFaqCategoryIn) = writeGetFaqCategoryIn(w, m)

    private fun readGetFaqCategoryIn(r: PacketReader): GetFaqCategoryIn {
        val vInt1 = r.int()
        return GetFaqCategoryIn(vInt1)
    }

    private fun writeGetFaqCategoryIn(w: PacketWriter, m: GetFaqCategoryIn) {
        w.int(m.int1)
    }
}
