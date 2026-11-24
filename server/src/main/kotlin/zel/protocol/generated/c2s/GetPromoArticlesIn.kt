package zel.protocol.generated

data object GetPromoArticlesIn

object GetPromoArticlesInCodec : IncomingCodec<GetPromoArticlesIn> {
    override val name = "GetPromoArticlesIn"

    override fun read(r: PacketReader): GetPromoArticlesIn = GetPromoArticlesIn

    override fun write(w: PacketWriter, m: GetPromoArticlesIn) = writeGetPromoArticlesIn(w, m)

    @Suppress("UNUSED_PARAMETER")
    private fun writeGetPromoArticlesIn(w: PacketWriter, m: GetPromoArticlesIn) {
    }
}
