package zel.protocol.generated

data object GetFaqCategoriesIn

object GetFaqCategoriesInCodec : IncomingCodec<GetFaqCategoriesIn> {
    override val name = "GetFaqCategoriesIn"

    override fun read(r: PacketReader): GetFaqCategoriesIn = GetFaqCategoriesIn

    override fun write(w: PacketWriter, m: GetFaqCategoriesIn) = writeGetFaqCategoriesIn(w, m)

    @Suppress("UNUSED_PARAMETER")
    private fun writeGetFaqCategoriesIn(w: PacketWriter, m: GetFaqCategoriesIn) {
    }
}
